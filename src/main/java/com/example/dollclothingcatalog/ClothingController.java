package com.example.dollclothingcatalog;

import static com.example.dollclothingcatalog.CatalogForms.ClothingDesignForm;
import static com.example.dollclothingcatalog.CatalogForms.OwnedItemForm;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClothingController {
	private final CatalogService catalog;

	public ClothingController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/clothes")
	public String list(@RequestParam(required = false) String name,
			@RequestParam(required = false) Long brandId,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) String size,
			@RequestParam(required = false) String setName,
			@RequestParam(required = false) Long dollId,
			@RequestParam(required = false) String colorName,
			Model model) {
		var filters = new CatalogService.ClothingFilters(name, brandId, categoryId, size,
				setName, dollId, colorName);
		model.addAttribute("clothes", catalog.clothing(filters));
		model.addAttribute("filters", filters);
		addOptions(model);
		model.addAttribute("pageTitle", "お洋服を探す");
		return "clothes/list";
	}

	@GetMapping("/designs")
	public String designs(Model model) {
		model.addAttribute("designs", catalog.designs());
		model.addAttribute("pageTitle", "商品デザイン");
		return "clothes/designs";
	}

	@GetMapping("/designs/new")
	public String createDesignForm(Model model) {
		model.addAttribute("designForm", new ClothingDesignForm());
		model.addAttribute("formTitle", "商品デザインを登録");
		addDesignOptions(model);
		return "clothes/design-form";
	}

	@PostMapping("/designs")
	public String createDesign(@Valid @ModelAttribute("designForm") ClothingDesignForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("formTitle", "商品デザインを登録");
			addDesignOptions(model);
			return "clothes/design-form";
		}
		try {
			long id = catalog.saveDesign(null, form);
			redirect.addFlashAttribute("successMessage", "商品デザインを登録しました。続けて所持アイテムを追加できます。");
			return "redirect:/designs/" + id;
		} catch (IllegalArgumentException ex) {
			result.reject("invalidSelection", ex.getMessage());
			model.addAttribute("formTitle", "商品デザインを登録");
			addDesignOptions(model);
			return "clothes/design-form";
		}
	}

	@GetMapping("/designs/{id}")
	public String designDetail(@PathVariable long id, Model model) {
		model.addAttribute("design", catalog.design(id));
		model.addAttribute("ownedItems", catalog.clothingForDesign(id));
		model.addAttribute("pageTitle", "商品デザイン詳細");
		return "clothes/design-detail";
	}

	@GetMapping("/designs/{id}/edit")
	public String editDesignForm(@PathVariable long id, Model model) {
		var design = catalog.design(id);
		ClothingDesignForm form = new ClothingDesignForm();
		form.setName(design.name());
		form.setBrandName(design.brandName());
		form.setCategoryName(design.categoryName());
		form.setSetName(design.setName());
		form.setDescription(design.description());
		model.addAttribute("designForm", form);
		model.addAttribute("designId", id);
		model.addAttribute("formTitle", "商品デザインを編集");
		addDesignOptions(model);
		return "clothes/design-form";
	}

	@PostMapping("/designs/{id}")
	public String updateDesign(@PathVariable long id,
			@Valid @ModelAttribute("designForm") ClothingDesignForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("designId", id);
			model.addAttribute("formTitle", "商品デザインを編集");
			addDesignOptions(model);
			return "clothes/design-form";
		}
		try {
			catalog.saveDesign(id, form);
			redirect.addFlashAttribute("successMessage", "商品デザインを更新しました。");
			return "redirect:/designs/" + id;
		} catch (IllegalArgumentException ex) {
			result.reject("invalidSelection", ex.getMessage());
			model.addAttribute("designId", id);
			model.addAttribute("formTitle", "商品デザインを編集");
			addDesignOptions(model);
			return "clothes/design-form";
		}
	}

	@PostMapping("/designs/{id}/delete")
	public String deleteDesign(@PathVariable long id, RedirectAttributes redirect) {
		if (!catalog.deleteDesign(id)) {
			redirect.addFlashAttribute("errorMessage",
					"所持アイテムが登録されている商品デザインは削除できません。先に所持アイテムを削除してください。");
			return "redirect:/designs/" + id;
		}
		redirect.addFlashAttribute("successMessage", "商品デザインを削除しました。");
		return "redirect:/designs";
	}

	@GetMapping("/clothes/new")
	public String createForm(@RequestParam(required = false) Long designId, Model model) {
		OwnedItemForm form = new OwnedItemForm();
		form.setDesignId(designId);
		model.addAttribute("ownedItemForm", form);
		model.addAttribute("formTitle", "所持アイテムを追加");
		addOptions(model);
		return "clothes/form";
	}

	@PostMapping("/clothes")
	public String create(@Valid @ModelAttribute("ownedItemForm") OwnedItemForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("formTitle", "所持アイテムを追加");
			addOptions(model);
			return "clothes/form";
		}
		try {
			long id = catalog.saveOwnedItem(null, form);
			redirect.addFlashAttribute("successMessage", "所持アイテムを登録しました。");
			return "redirect:/clothes/" + id;
		} catch (IllegalArgumentException ex) {
			result.reject("invalidSelection", ex.getMessage());
			model.addAttribute("formTitle", "所持アイテムを追加");
			addOptions(model);
			return "clothes/form";
		}
	}

	@GetMapping("/clothes/{id}")
	public String detail(@PathVariable long id, Model model) {
		var clothing = catalog.clothingItem(id);
		model.addAttribute("clothing", clothing);
		model.addAttribute("savedPalettes", catalog.savedColorPalettes(id));
		model.addAttribute("paletteSearchQuery",
				clothing.colorName() != null ? clothing.colorName() : "");
		model.addAttribute("pageTitle", "所持アイテム詳細");
		return "clothes/detail";
	}

	@GetMapping("/clothes/{id}/edit")
	public String editForm(@PathVariable long id, Model model) {
		var item = catalog.clothingItem(id);
		OwnedItemForm form = new OwnedItemForm();
		form.setDesignId(item.designId());
		form.setColorName(item.colorName());
		form.setColorHex(item.colorHex());
		form.setSize(item.size());
		form.setNotes(item.notes());
		form.setDollIds(item.dolls().stream().map(CatalogModels.Doll::id).toList());
		model.addAttribute("ownedItemForm", form);
		model.addAttribute("clothingId", id);
		model.addAttribute("formTitle", "所持アイテムを編集");
		addOptions(model);
		return "clothes/form";
	}

	@PostMapping("/clothes/{id}")
	public String update(@PathVariable long id,
			@Valid @ModelAttribute("ownedItemForm") OwnedItemForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("clothingId", id);
			model.addAttribute("formTitle", "所持アイテムを編集");
			addOptions(model);
			return "clothes/form";
		}
		try {
			catalog.saveOwnedItem(id, form);
			redirect.addFlashAttribute("successMessage", "所持アイテムを更新しました。");
			return "redirect:/clothes/" + id;
		} catch (IllegalArgumentException ex) {
			result.reject("invalidSelection", ex.getMessage());
			model.addAttribute("clothingId", id);
			model.addAttribute("formTitle", "所持アイテムを編集");
			addOptions(model);
			return "clothes/form";
		}
	}

	@PostMapping("/clothes/{id}/delete")
	public String delete(@PathVariable long id, RedirectAttributes redirect) {
		catalog.deleteClothing(id);
		redirect.addFlashAttribute("successMessage", "所持アイテムを削除しました。");
		return "redirect:/clothes";
	}

	@GetMapping("/sets")
	public String sets(Model model) {
		model.addAttribute("sets", catalog.clothingSets());
		model.addAttribute("pageTitle", "フルセット一覧");
		return "clothes/sets";
	}

	@GetMapping("/sets/{brandId}")
	public String setDetail(@PathVariable long brandId, @RequestParam String name, Model model) {
		model.addAttribute("clothes", catalog.clothingSet(brandId, name));
		model.addAttribute("setName", name);
		model.addAttribute("pageTitle", name);
		return "clothes/set-detail";
	}

	private void addOptions(Model model) {
		model.addAttribute("brands", catalog.brands());
		model.addAttribute("categories", catalog.categories());
		model.addAttribute("designs", catalog.designs());
		model.addAttribute("dolls", catalog.dolls());
	}

	private void addDesignOptions(Model model) {
		model.addAttribute("brands", catalog.brands());
		model.addAttribute("categories", catalog.categories());
	}
}
