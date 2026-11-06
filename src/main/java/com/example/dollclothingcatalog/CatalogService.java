package com.example.dollclothingcatalog;

import static com.example.dollclothingcatalog.CatalogModels.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {
	private static final RowMapper<Doll> DOLL_ROW = (rs, row) -> mapDoll(rs);
	private static final RowMapper<Brand> BRAND_ROW = (rs, row) ->
			new Brand(rs.getLong("Brand_ID"), rs.getString("Brand_Name"),
					rs.getString("Website_URL"), rs.getString("Notes"));
	private static final RowMapper<Category> CATEGORY_ROW = (rs, row) ->
			new Category(rs.getLong("Category_ID"), rs.getString("Category_Name"),
					rs.getString("Description"));
	private static final String DESIGN_SELECT = """
			SELECT d."Clothing_Design_ID", d."Design_Name", d."Brand_ID", b."Brand_Name",
			       d."Category_ID", c."Category_Name", d."Set_Name", d."Description",
			       d."Created_At", d."Updated_At", count(i."Clothing_Item_ID") AS owned_item_count
			FROM "Clothing_Designs" d
			JOIN "Brands" b ON b."Brand_ID" = d."Brand_ID"
			JOIN "Categories" c ON c."Category_ID" = d."Category_ID"
			""";
	private static final String CLOTHING_SELECT = """
			SELECT i."Clothing_Item_ID", d."Clothing_Design_ID", d."Design_Name",
			       d."Brand_ID", b."Brand_Name", d."Category_ID", c."Category_Name",
			       d."Set_Name", d."Description", i."Color_Name", i."Color_Hex",
			       i."Size", i."Notes", i."Created_At", i."Updated_At"
			FROM "Clothing_Items" i
			JOIN "Clothing_Designs" d ON d."Clothing_Design_ID" = i."Clothing_Design_ID"
			JOIN "Brands" b ON b."Brand_ID" = d."Brand_ID"
			JOIN "Categories" c ON c."Category_ID" = d."Category_ID"
			""";

	private final JdbcTemplate jdbc;
	private final ObjectMapper objectMapper;

	public CatalogService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
		this.jdbc = jdbc;
		this.objectMapper = objectMapper;
	}

	public List<Doll> dolls() {
		return jdbc.query("""
				SELECT "Doll_ID", "Doll_Name", "Doll_Maker", "Body_Name", "Body_Size",
				       "Wig_Size", "Eye_Size", "Notes"
				FROM "Dolls" ORDER BY "Doll_Name", "Doll_ID"
				""", DOLL_ROW);
	}

	public Doll doll(long id) {
		return jdbc.queryForObject("""
				SELECT "Doll_ID", "Doll_Name", "Doll_Maker", "Body_Name", "Body_Size",
				       "Wig_Size", "Eye_Size", "Notes"
				FROM "Dolls" WHERE "Doll_ID" = ?
				""", DOLL_ROW, id);
	}

	public long saveDoll(Long id, CatalogForms.DollForm form) {
		if (id == null) {
			return jdbc.queryForObject("""
					INSERT INTO "Dolls" ("Doll_Name", "Doll_Maker", "Body_Name", "Body_Size",
					                     "Wig_Size", "Eye_Size", "Notes")
					VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING "Doll_ID"
					""", Long.class, form.getName().trim(), blankToNull(form.getMaker()),
					blankToNull(form.getBodyName()), blankToNull(form.getBodySize()),
					blankToNull(form.getWigSize()), blankToNull(form.getEyeSize()),
					blankToNull(form.getNotes()));
		}
		int updated = jdbc.update("""
				UPDATE "Dolls" SET "Doll_Name" = ?, "Doll_Maker" = ?, "Body_Name" = ?,
				       "Body_Size" = ?, "Wig_Size" = ?, "Eye_Size" = ?, "Notes" = ?
				WHERE "Doll_ID" = ?
				""", form.getName().trim(), blankToNull(form.getMaker()),
				blankToNull(form.getBodyName()), blankToNull(form.getBodySize()),
				blankToNull(form.getWigSize()), blankToNull(form.getEyeSize()),
				blankToNull(form.getNotes()), id);
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return id;
	}

	public void deleteDoll(long id) {
		jdbc.update("DELETE FROM \"Dolls\" WHERE \"Doll_ID\" = ?", id);
	}

	public List<Brand> brands() {
		return jdbc.query("""
				SELECT "Brand_ID", "Brand_Name", "Website_URL", "Notes"
				FROM "Brands" ORDER BY "Brand_Name", "Brand_ID"
				""", BRAND_ROW);
	}

	public void deleteBrand(long id) {
		jdbc.update("DELETE FROM \"Brands\" WHERE \"Brand_ID\" = ?", id);
	}

	public List<Category> categories() {
		return jdbc.query("""
				SELECT "Category_ID", "Category_Name", "Description"
				FROM "Categories" ORDER BY "Category_Name", "Category_ID"
				""", CATEGORY_ROW);
	}

	public List<ClothingDesign> designs() {
		return jdbc.query(DESIGN_SELECT + """
				LEFT JOIN "Clothing_Items" i ON i."Clothing_Design_ID" = d."Clothing_Design_ID"
				GROUP BY d."Clothing_Design_ID", d."Design_Name", d."Brand_ID", b."Brand_Name",
				         d."Category_ID", c."Category_Name", d."Set_Name", d."Description",
				         d."Created_At", d."Updated_At"
				ORDER BY d."Design_Name", d."Clothing_Design_ID"
				""", (rs, row) -> mapDesign(rs, rs.getLong("owned_item_count")),
				new Object[0]);
	}

	public ClothingDesign design(long id) {
		return jdbc.queryForObject(DESIGN_SELECT + """
				LEFT JOIN "Clothing_Items" i ON i."Clothing_Design_ID" = d."Clothing_Design_ID"
				WHERE d."Clothing_Design_ID" = ?
				GROUP BY d."Clothing_Design_ID", d."Design_Name", d."Brand_ID", b."Brand_Name",
				         d."Category_ID", c."Category_Name", d."Set_Name", d."Description",
				         d."Created_At", d."Updated_At"
				""", (rs, row) -> mapDesign(rs, rs.getLong("owned_item_count")), id);
	}

	public List<ClothingItem> clothing(ClothingFilters filters) {
		StringBuilder sql = new StringBuilder(CLOTHING_SELECT).append(" WHERE 1 = 1");
		List<Object> args = new ArrayList<>();
		if (hasText(filters.name())) {
			sql.append(" AND d.\"Design_Name\" ILIKE ? ESCAPE '\\'");
			args.add("%" + escapeLike(filters.name().trim()) + "%");
		}
		if (filters.brandId() != null) {
			sql.append(" AND d.\"Brand_ID\" = ?");
			args.add(filters.brandId());
		}
		if (filters.categoryId() != null) {
			sql.append(" AND d.\"Category_ID\" = ?");
			args.add(filters.categoryId());
		}
		if (hasText(filters.size())) {
			sql.append(" AND i.\"Size\" ILIKE ? ESCAPE '\\'");
			args.add("%" + escapeLike(filters.size().trim()) + "%");
		}
		if (hasText(filters.setName())) {
			sql.append(" AND d.\"Set_Name\" ILIKE ? ESCAPE '\\'");
			args.add("%" + escapeLike(filters.setName().trim()) + "%");
		}
		if (filters.dollId() != null) {
			sql.append("""
					 AND EXISTS (SELECT 1 FROM "Clothing_Item_Dolls" link
					             WHERE link."Clothing_Item_ID" = i."Clothing_Item_ID"
					               AND link."Doll_ID" = ?)
					""");
			args.add(filters.dollId());
		}
		if (hasText(filters.colorName())) {
			sql.append(" AND i.\"Color_Name\" ILIKE ? ESCAPE '\\'");
			args.add("%" + escapeLike(filters.colorName().trim()) + "%");
		}
		sql.append(" ORDER BY d.\"Design_Name\", i.\"Color_Name\" NULLS FIRST, i.\"Clothing_Item_ID\"");
		return jdbc.query(sql.toString(), this::mapClothing, args.toArray());
	}

	public ClothingItem clothingItem(long id) {
		ClothingItem item = jdbc.queryForObject(CLOTHING_SELECT
				+ " WHERE i.\"Clothing_Item_ID\" = ?", this::mapClothing, id);
		return withDolls(item);
	}

	public List<ClothingItem> clothingForDoll(long dollId) {
		return jdbc.query(CLOTHING_SELECT + """
				 WHERE EXISTS (SELECT 1 FROM "Clothing_Item_Dolls" link
				               WHERE link."Clothing_Item_ID" = i."Clothing_Item_ID"
				                 AND link."Doll_ID" = ?)
				 ORDER BY d."Design_Name", i."Color_Name" NULLS FIRST, i."Clothing_Item_ID"
				""", this::mapClothing, dollId);
	}

	public List<ClothingItem> clothingForDesign(long designId) {
		List<ClothingItem> items = jdbc.query(CLOTHING_SELECT + """
				 WHERE d."Clothing_Design_ID" = ?
				 ORDER BY i."Color_Name" NULLS FIRST, i."Clothing_Item_ID"
				""", this::mapClothing, designId);
		return items.stream().map(this::withDolls).toList();
	}

	@Transactional
	public long saveDesign(Long id, CatalogForms.ClothingDesignForm form) {
		long brandId = resolveBrandId(form.getBrandName());
		long categoryId = resolveCategoryId(form.getCategoryName());
		String name = form.getName().trim();
		String setName = blankToNull(form.getSetName());
		String description = blankToNull(form.getDescription());
		if (id == null) {
			return jdbc.queryForObject("""
					INSERT INTO "Clothing_Designs" ("Design_Name", "Brand_ID", "Category_ID",
					                                "Set_Name", "Description")
					VALUES (?, ?, ?, ?, ?) RETURNING "Clothing_Design_ID"
					""", Long.class, name, brandId, categoryId, setName, description);
		}
		int updated = jdbc.update("""
				UPDATE "Clothing_Designs" SET "Design_Name" = ?, "Brand_ID" = ?,
				       "Category_ID" = ?, "Set_Name" = ?, "Description" = ?,
				       "Updated_At" = CURRENT_TIMESTAMP
				WHERE "Clothing_Design_ID" = ?
				""", name, brandId, categoryId, setName, description, id);
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return id;
	}

	@Transactional
	public long saveOwnedItem(Long id, CatalogForms.OwnedItemForm form) {
		Set<Long> selectedDolls = new LinkedHashSet<>(form.getDollIds());
		Long designId = form.getDesignId();
		if (designId == null || !Boolean.TRUE.equals(jdbc.queryForObject("""
				SELECT EXISTS (SELECT 1 FROM "Clothing_Designs" WHERE "Clothing_Design_ID" = ?)
				""", Boolean.class, designId))) {
			throw new IllegalArgumentException("選択された商品デザインが見つかりません。");
		}
		if (!selectedDolls.isEmpty()) {
			String placeholders = String.join(",", selectedDolls.stream().map(value -> "?").toList());
			Integer found = jdbc.queryForObject("SELECT count(*) FROM \"Dolls\" WHERE \"Doll_ID\" IN ("
					+ placeholders + ")", Integer.class, selectedDolls.toArray());
			if (found == null || found != selectedDolls.size()) {
				throw new IllegalArgumentException("選択されたドールが見つかりません。");
			}
		}
		String colorName = blankToNull(form.getColorName());
		String colorHex = blankToNull(form.getColorHex());
		if (colorHex != null) {
			colorHex = colorHex.toUpperCase(Locale.ROOT);
		}
		long itemId;
		if (id == null) {
			itemId = jdbc.queryForObject("""
					INSERT INTO "Clothing_Items" ("Clothing_Design_ID", "Color_Name",
					                              "Color_Hex", "Size", "Notes")
					VALUES (?, ?, ?, ?, ?) RETURNING "Clothing_Item_ID"
					""", Long.class, designId, colorName, colorHex,
					blankToNull(form.getSize()), blankToNull(form.getNotes()));
		} else {
			int updated = jdbc.update("""
					UPDATE "Clothing_Items" SET "Clothing_Design_ID" = ?, "Color_Name" = ?,
					       "Color_Hex" = ?, "Size" = ?, "Notes" = ?,
					       "Updated_At" = CURRENT_TIMESTAMP
					WHERE "Clothing_Item_ID" = ?
					""", designId, colorName, colorHex, blankToNull(form.getSize()),
					blankToNull(form.getNotes()), id);
			if (updated == 0) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			itemId = id;
			jdbc.update("DELETE FROM \"Clothing_Item_Dolls\" WHERE \"Clothing_Item_ID\" = ?", itemId);
		}
		for (Long dollId : selectedDolls) {
			jdbc.update("""
					INSERT INTO "Clothing_Item_Dolls" ("Clothing_Item_ID", "Doll_ID")
					VALUES (?, ?)
					""", itemId, dollId);
		}
		return itemId;
	}

	public boolean deleteDesign(long id) {
		Integer ownedItems = jdbc.queryForObject("""
				SELECT count(*) FROM "Clothing_Items" WHERE "Clothing_Design_ID" = ?
				""", Integer.class, id);
		if (ownedItems != null && ownedItems > 0) {
			return false;
		}
		return jdbc.update("DELETE FROM \"Clothing_Designs\" WHERE \"Clothing_Design_ID\" = ?", id) > 0;
	}

	@Transactional
	public void deleteClothing(long id) {
		jdbc.update("DELETE FROM \"Clothing_Item_Dolls\" WHERE \"Clothing_Item_ID\" = ?", id);
		jdbc.update("DELETE FROM \"Clothing_Items\" WHERE \"Clothing_Item_ID\" = ?", id);
	}

	public List<ClothingSet> clothingSets() {
		return jdbc.query("""
				SELECT d."Brand_ID", b."Brand_Name", d."Set_Name", count(*) AS design_count
				FROM "Clothing_Designs" d JOIN "Brands" b ON b."Brand_ID" = d."Brand_ID"
				WHERE d."Set_Name" IS NOT NULL AND btrim(d."Set_Name") <> ''
				GROUP BY d."Brand_ID", b."Brand_Name", d."Set_Name"
				ORDER BY b."Brand_Name", d."Set_Name"
				""", (rs, row) -> new ClothingSet(rs.getLong("Brand_ID"),
				rs.getString("Brand_Name"), rs.getString("Set_Name"), rs.getLong("design_count")));
	}

	public List<ClothingItem> clothingSet(long brandId, String setName) {
		return jdbc.query(CLOTHING_SELECT + """
				 WHERE d."Brand_ID" = ? AND d."Set_Name" = ?
				 ORDER BY d."Design_Name", i."Color_Name" NULLS FIRST, i."Clothing_Item_ID"
				""", this::mapClothing, brandId, setName);
	}

	public long dollCount() {
		return jdbc.queryForObject("SELECT count(*) FROM \"Dolls\"", Long.class);
	}

	public long clothingCount() {
		return jdbc.queryForObject("SELECT count(*) FROM \"Clothing_Items\"", Long.class);
	}

	public long designCount() {
		return jdbc.queryForObject("SELECT count(*) FROM \"Clothing_Designs\"", Long.class);
	}

	public boolean brandInUse(long id) {
		return jdbc.queryForObject("""
				SELECT EXISTS (SELECT 1 FROM "Clothing_Designs" WHERE "Brand_ID" = ?)
				""", Boolean.class, id);
	}

	public List<SavedColorPalette> savedColorPalettes(long clothingItemId) {
		return jdbc.query("""
				SELECT "Saved_Color_Palette_ID", "Clothing_Item_ID", "ColorMagic_Palette_ID",
				       "Palette_Name", "Colors", "Tags", "Saved_At"
				FROM "Saved_Color_Palettes"
				WHERE "Clothing_Item_ID" = ?
				ORDER BY "Saved_At" DESC, "Saved_Color_Palette_ID" DESC
				""", (rs, row) -> new SavedColorPalette(
						rs.getLong("Saved_Color_Palette_ID"), rs.getLong("Clothing_Item_ID"),
						rs.getString("ColorMagic_Palette_ID"), rs.getString("Palette_Name"),
						readJsonList(rs.getString("Colors")), readJsonList(rs.getString("Tags")),
						rs.getObject("Saved_At", java.time.OffsetDateTime.class)),
				clothingItemId);
	}

	@Transactional
	public SavedPaletteSaveResult saveColorPalette(long clothingItemId, ColorPalette palette) {
		validateColorPalette(palette);
		int inserted = jdbc.update("""
				INSERT INTO "Saved_Color_Palettes" (
					"Clothing_Item_ID", "ColorMagic_Palette_ID", "Palette_Name", "Colors", "Tags"
				)
				SELECT ?, ?, ?, CAST(? AS JSONB), CAST(? AS JSONB)
				WHERE EXISTS (
					SELECT 1 FROM "Clothing_Items" WHERE "Clothing_Item_ID" = ?
				)
				ON CONFLICT ("Clothing_Item_ID", "ColorMagic_Palette_ID") DO NOTHING
				""", clothingItemId, palette.id(), palette.text(),
				writeJson(palette.colors()), writeJson(palette.tags()), clothingItemId);
		if (inserted == 0 && !ownedItemExists(clothingItemId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		SavedColorPalette saved = jdbc.queryForObject("""
				SELECT "Saved_Color_Palette_ID", "Clothing_Item_ID", "ColorMagic_Palette_ID",
				       "Palette_Name", "Colors", "Tags", "Saved_At"
				FROM "Saved_Color_Palettes"
				WHERE "Clothing_Item_ID" = ? AND "ColorMagic_Palette_ID" = ?
				""", (rs, row) -> new SavedColorPalette(
						rs.getLong("Saved_Color_Palette_ID"), rs.getLong("Clothing_Item_ID"),
						rs.getString("ColorMagic_Palette_ID"), rs.getString("Palette_Name"),
						readJsonList(rs.getString("Colors")), readJsonList(rs.getString("Tags")),
						rs.getObject("Saved_At", java.time.OffsetDateTime.class)),
				clothingItemId, palette.id());
		return new SavedPaletteSaveResult(saved, inserted == 0);
	}

	public boolean deleteColorPalette(long clothingItemId, long savedPaletteId) {
		return jdbc.update("""
				DELETE FROM "Saved_Color_Palettes"
				WHERE "Clothing_Item_ID" = ? AND "Saved_Color_Palette_ID" = ?
				""", clothingItemId, savedPaletteId) > 0;
	}

	private boolean ownedItemExists(long clothingItemId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
				SELECT EXISTS (SELECT 1 FROM "Clothing_Items" WHERE "Clothing_Item_ID" = ?)
				""", Boolean.class, clothingItemId));
	}

	private static void validateColorPalette(ColorPalette palette) {
		if (palette == null || palette.id() == null || palette.id().isBlank()
				|| palette.id().length() > 100 || palette.text() == null
				|| palette.text().isBlank() || palette.text().length() > 200
				|| palette.colors() == null || palette.colors().isEmpty()
				|| palette.colors().size() > 20 || palette.tags() == null
				|| palette.tags().size() > 30
				|| palette.colors().stream().anyMatch(color ->
						color == null || !color.matches("^#[0-9A-Fa-f]{6}$"))
				|| palette.tags().stream().anyMatch(tag ->
						tag == null || tag.length() > 100)) {
			throw new IllegalArgumentException("保存するパレットの情報が正しくありません。");
		}
	}

	private String writeJson(List<String> values) {
		try {
			return objectMapper.writeValueAsString(values);
		} catch (JacksonException exception) {
			throw new IllegalStateException("配色パレットを保存形式に変換できませんでした。", exception);
		}
	}

	private List<String> readJsonList(String value) {
		try {
			return objectMapper.readValue(value, new TypeReference<>() { });
		} catch (JacksonException exception) {
			throw new IllegalStateException("保存済み配色パレットを読み取れませんでした。", exception);
		}
	}

	private long resolveBrandId(String value) {
		String brandName = blankToNull(value);
		if (brandName == null) {
			throw new IllegalArgumentException("ブランド・購入元を入力してください。");
		}
		List<Long> existingIds = jdbc.query("""
				SELECT "Brand_ID" FROM "Brands"
				WHERE lower("Brand_Name") = lower(?)
				ORDER BY "Brand_ID" LIMIT 1
				""", (rs, row) -> rs.getLong("Brand_ID"), brandName);
		if (!existingIds.isEmpty()) {
			return existingIds.get(0);
		}
		return jdbc.queryForObject("""
				INSERT INTO "Brands" ("Brand_Name") VALUES (?)
				ON CONFLICT ("Brand_Name") DO UPDATE SET "Brand_Name" = EXCLUDED."Brand_Name"
				RETURNING "Brand_ID"
				""", Long.class, brandName);
	}

	private long resolveCategoryId(String value) {
		String categoryName = blankToNull(value);
		if (categoryName == null) {
			throw new IllegalArgumentException("カテゴリを入力してください。");
		}
		List<Long> existingIds = jdbc.query("""
				SELECT "Category_ID" FROM "Categories"
				WHERE lower("Category_Name") = lower(?)
				ORDER BY "Category_ID" LIMIT 1
				""", (rs, row) -> rs.getLong("Category_ID"), categoryName);
		if (!existingIds.isEmpty()) {
			return existingIds.get(0);
		}
		return jdbc.queryForObject("""
				INSERT INTO "Categories" ("Category_Name") VALUES (?)
				ON CONFLICT ("Category_Name") DO UPDATE SET "Category_Name" = EXCLUDED."Category_Name"
				RETURNING "Category_ID"
				""", Long.class, categoryName);
	}

	private ClothingItem withDolls(ClothingItem item) {
		List<Doll> dolls = jdbc.query("""
				SELECT d."Doll_ID", d."Doll_Name", d."Doll_Maker", d."Body_Name",
				       d."Body_Size", d."Wig_Size", d."Eye_Size", d."Notes"
				FROM "Dolls" d
				JOIN "Clothing_Item_Dolls" link ON link."Doll_ID" = d."Doll_ID"
				WHERE link."Clothing_Item_ID" = ?
				ORDER BY d."Doll_Name", d."Doll_ID"
				""", DOLL_ROW, item.id());
		return new ClothingItem(item.id(), item.designId(), item.name(), item.brandId(), item.brandName(),
				item.categoryId(), item.categoryName(), item.setName(), item.description(),
				item.colorName(), item.colorHex(), item.size(), item.notes(),
				item.createdAt(), item.updatedAt(), dolls);
	}

	private ClothingDesign mapDesign(ResultSet rs, long ownedItemCount) throws SQLException {
		return new ClothingDesign(rs.getLong("Clothing_Design_ID"), rs.getString("Design_Name"),
				rs.getLong("Brand_ID"), rs.getString("Brand_Name"), rs.getLong("Category_ID"),
				rs.getString("Category_Name"), rs.getString("Set_Name"), rs.getString("Description"),
				rs.getObject("Created_At", java.time.OffsetDateTime.class),
				rs.getObject("Updated_At", java.time.OffsetDateTime.class), ownedItemCount);
	}

	private ClothingItem mapClothing(ResultSet rs, int row) throws SQLException {
		return new ClothingItem(rs.getLong("Clothing_Item_ID"), rs.getLong("Clothing_Design_ID"),
				rs.getString("Design_Name"), rs.getLong("Brand_ID"), rs.getString("Brand_Name"),
				rs.getLong("Category_ID"), rs.getString("Category_Name"), rs.getString("Set_Name"),
				rs.getString("Description"), rs.getString("Color_Name"), rs.getString("Color_Hex"),
				rs.getString("Size"), rs.getString("Notes"),
				rs.getObject("Created_At", java.time.OffsetDateTime.class),
				rs.getObject("Updated_At", java.time.OffsetDateTime.class), List.of());
	}

	private static Doll mapDoll(ResultSet rs) throws SQLException {
		return new Doll(rs.getLong("Doll_ID"), rs.getString("Doll_Name"),
				rs.getString("Doll_Maker"), rs.getString("Body_Name"),
				rs.getString("Body_Size"), rs.getString("Wig_Size"),
				rs.getString("Eye_Size"), rs.getString("Notes"));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	public record ClothingFilters(String name, Long brandId, Long categoryId, String size,
			String setName, Long dollId, String colorName) {
	}

	public record SavedPaletteSaveResult(SavedColorPalette palette, boolean alreadySaved) {
	}
}
