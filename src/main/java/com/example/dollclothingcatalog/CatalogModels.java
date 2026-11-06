package com.example.dollclothingcatalog;

import java.time.OffsetDateTime;
import java.util.List;

public final class CatalogModels {
	private CatalogModels() {
	}

	public record Doll(long id, String name, String maker, String bodyName, String bodySize,
			String wigSize, String eyeSize, String notes) {
	}

	public record Brand(long id, String name, String websiteUrl, String notes) {
	}

	public record Category(long id, String name, String description) {
	}

	public record ClothingDesign(long id, String name, long brandId, String brandName,
			long categoryId, String categoryName, String setName, String description,
			OffsetDateTime createdAt, OffsetDateTime updatedAt, long ownedItemCount) {
	}

	public record ClothingItem(long id, long designId, String name, long brandId, String brandName,
			long categoryId, String categoryName, String setName, String description,
			String colorName, String colorHex, String size, String notes,
			OffsetDateTime createdAt, OffsetDateTime updatedAt, List<Doll> dolls) {
	}

	public record ColorPalette(String id, String text, List<String> colors, List<String> tags) {
	}

	public record SavedColorPalette(long id, long clothingItemId, String paletteId, String text,
			List<String> colors, List<String> tags, OffsetDateTime savedAt) {
	}

	public record Option(long id, String name) {
	}

	public record ClothingSet(long brandId, String brandName, String name, long itemCount) {
	}
}
