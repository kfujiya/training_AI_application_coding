package com.example.dollclothingcatalog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class CatalogFormsTests {
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void dollNameIsRequiredAndLimitedToOneHundredCharacters() {
		var form = new CatalogForms.DollForm();
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("name")));

		form.setName("あ".repeat(101));
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("name")));
	}

	@Test
	void clothingDesignRequiresBrandAndCategory() {
		var form = new CatalogForms.ClothingDesignForm();
		form.setName("ワンピース");
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("brandName")));
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("categoryName")));
		form.setBrandName("登録済みブランド");
		form.setCategoryName("ドレス");
		assertTrue(validator.validate(form).stream()
				.noneMatch(error -> error.getPropertyPath().toString().equals("brandName")));
		assertTrue(validator.validate(form).stream()
				.noneMatch(error -> error.getPropertyPath().toString().equals("categoryName")));
		assertTrue(validator.validate(form).isEmpty());
		form.setCategoryName("新しいカテゴリ");
		assertTrue(validator.validate(form).isEmpty());
		form.setCategoryName("あ".repeat(101));
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("categoryName")));
		form.setBrandName("新しい購入元");
		assertTrue(validator.validate(form).stream()
				.noneMatch(error -> error.getPropertyPath().toString().equals("brandName")));
		form.setBrandName("あ".repeat(101));
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("brandName")));
	}

	@Test
	void ownedItemAllowsOptionalColorAndDollsButValidatesHexColor() {
		var form = new CatalogForms.OwnedItemForm();
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("designId")));
		form.setDesignId(1L);
		assertTrue(form.getDollIds().isEmpty());
		assertTrue(validator.validate(form).isEmpty());

		form.setColorHex("#36bFb0");
		assertTrue(validator.validate(form).isEmpty());
		form.setColorHex("36BFB0");
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("colorHex")));
		form.setColorHex("#12345G");
		assertTrue(validator.validate(form).stream()
				.anyMatch(error -> error.getPropertyPath().toString().equals("colorHex")));
		form.setColorHex("");
		assertTrue(validator.validate(form).isEmpty());
	}
}
