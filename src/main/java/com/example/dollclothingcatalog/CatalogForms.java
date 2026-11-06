package com.example.dollclothingcatalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public final class CatalogForms {
	private CatalogForms() {
	}

	public static class DollForm {
		@NotBlank(message = "ドール名を入力してください。")
		@Size(max = 100, message = "ドール名は100文字以内で入力してください。")
		private String name;
		@Size(max = 100, message = "メーカーは100文字以内で入力してください。")
		private String maker;
		@Size(max = 100, message = "ボディ名は100文字以内で入力してください。")
		private String bodyName;
		@Size(max = 50, message = "ボディサイズは50文字以内で入力してください。")
		private String bodySize;
		@Size(max = 50, message = "ウィッグサイズは50文字以内で入力してください。")
		private String wigSize;
		@Size(max = 50, message = "アイサイズは50文字以内で入力してください。")
		private String eyeSize;
		private String notes;

		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public String getMaker() { return maker; }
		public void setMaker(String maker) { this.maker = maker; }
		public String getBodyName() { return bodyName; }
		public void setBodyName(String bodyName) { this.bodyName = bodyName; }
		public String getBodySize() { return bodySize; }
		public void setBodySize(String bodySize) { this.bodySize = bodySize; }
		public String getWigSize() { return wigSize; }
		public void setWigSize(String wigSize) { this.wigSize = wigSize; }
		public String getEyeSize() { return eyeSize; }
		public void setEyeSize(String eyeSize) { this.eyeSize = eyeSize; }
		public String getNotes() { return notes; }
		public void setNotes(String notes) { this.notes = notes; }
	}

	public static class ClothingDesignForm {
		@NotBlank(message = "お洋服の名前を入力してください。")
		@Size(max = 150, message = "お洋服の名前は150文字以内で入力してください。")
		private String name;
		@NotBlank(message = "ブランド・購入元を選択または入力してください。")
		@Size(max = 100, message = "ブランド・購入元は100文字以内で入力してください。")
		private String brandName;
		@NotBlank(message = "カテゴリを選択または入力してください。")
		@Size(max = 100, message = "カテゴリは100文字以内で入力してください。")
		private String categoryName;
		@Size(max = 150, message = "セット名は150文字以内で入力してください。")
		private String setName;

		private String description;

		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public String getBrandName() { return brandName; }
		public void setBrandName(String brandName) { this.brandName = brandName; }
		public String getCategoryName() { return categoryName; }
		public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
		public String getSetName() { return setName; }
		public void setSetName(String setName) { this.setName = setName; }
		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
	}

	public static class OwnedItemForm {
		@NotNull(message = "商品デザインを選択してください。")
		private Long designId;
		@Size(max = 100, message = "カラー名は100文字以内で入力してください。")
		private String colorName;
		@Pattern(regexp = "(?i)^\\s*(#[0-9a-f]{6})?\\s*$",
				message = "カラーコードは #RRGGBB 形式で入力してください。")
		private String colorHex;
		@Size(max = 50, message = "サイズは50文字以内で入力してください。")
		private String size;
		private String notes;
		private List<Long> dollIds = new ArrayList<>();

		public Long getDesignId() { return designId; }
		public void setDesignId(Long designId) { this.designId = designId; }
		public String getColorName() { return colorName; }
		public void setColorName(String colorName) { this.colorName = colorName; }
		public String getColorHex() { return colorHex; }
		public void setColorHex(String colorHex) { this.colorHex = colorHex; }
		public String getSize() { return size; }
		public void setSize(String size) { this.size = size; }
		public String getNotes() { return notes; }
		public void setNotes(String notes) { this.notes = notes; }
		public List<Long> getDollIds() { return dollIds; }
		public void setDollIds(List<Long> dollIds) { this.dollIds = dollIds == null ? new ArrayList<>() : dollIds; }
	}

}
