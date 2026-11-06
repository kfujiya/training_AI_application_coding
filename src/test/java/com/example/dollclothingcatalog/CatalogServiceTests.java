package com.example.dollclothingcatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

class CatalogServiceTests {
	@Test
	void sameDesignCanHaveIndependentColorsAndDollLinks() {
		var jdbc = new RecordingJdbcTemplate();
		var service = new CatalogService(jdbc, new ObjectMapper());
		var black = new CatalogForms.OwnedItemForm();
		black.setDesignId(12L);
		black.setColorName("ブラック");
		black.setColorHex("#111111");
		black.setDollIds(List.of(3L));
		var mint = new CatalogForms.OwnedItemForm();
		mint.setDesignId(12L);
		mint.setColorName("ミント");
		mint.setColorHex("#36BFB0");

		long blackId = service.saveOwnedItem(null, black);
		long mintId = service.saveOwnedItem(null, mint);

		assertEquals(2, jdbc.ownedItemInserts.size());
		assertEquals(100L, blackId);
		assertEquals(101L, mintId);
		assertEquals(12L, jdbc.ownedItemInserts.get(0)[0]);
		assertEquals(12L, jdbc.ownedItemInserts.get(1)[0]);
		assertEquals("#111111", jdbc.ownedItemInserts.get(0)[2]);
		assertEquals("#36BFB0", jdbc.ownedItemInserts.get(1)[2]);
		assertEquals(List.of(new DollLink(blackId, 3L)), jdbc.dollLinks);
	}

	private static final class RecordingJdbcTemplate extends JdbcTemplate {
		private long nextId = 100;
		private final List<Object[]> ownedItemInserts = new ArrayList<>();
		private final List<DollLink> dollLinks = new ArrayList<>();

		@Override
		public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
			if (requiredType == Boolean.class) {
				return requiredType.cast(Boolean.TRUE);
			}
			if (requiredType == Integer.class) {
				return requiredType.cast(args.length);
			}
			if (requiredType == Long.class && sql.startsWith("INSERT INTO \"Clothing_Items\"")) {
				ownedItemInserts.add(args);
				return requiredType.cast(nextId++);
			}
			throw new UnsupportedOperationException("Unexpected query: " + sql);
		}

		@Override
		public int update(String sql, Object... args) {
			if (sql.startsWith("INSERT INTO \"Clothing_Item_Dolls\"")) {
				dollLinks.add(new DollLink((Long) args[0], (Long) args[1]));
			}
			return 1;
		}
	}

	private record DollLink(long itemId, long dollId) {
	}
}
