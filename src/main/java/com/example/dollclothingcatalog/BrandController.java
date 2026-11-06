package com.example.dollclothingcatalog;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BrandController {
	private final CatalogService catalog;

	public BrandController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/brands")
	public String list(Model model) {
		model.addAttribute("brands", catalog.brands());
		model.addAttribute("pageTitle", "ブランド・購入元");
		return "brands/list";
	}

	@PostMapping("/brands/{id}/delete")
	public String delete(@PathVariable long id, RedirectAttributes redirect) {
		if (catalog.brandInUse(id)) {
			redirect.addFlashAttribute("errorMessage", "登録済みのお洋服で使用中のブランドは削除できません。");
			return "redirect:/brands";
		}
		try {
			catalog.deleteBrand(id);
			redirect.addFlashAttribute("successMessage", "ブランド・購入元を削除しました。");
		} catch (DataIntegrityViolationException ex) {
			redirect.addFlashAttribute("errorMessage", "お洋服から参照されているため削除できません。");
		}
		return "redirect:/brands";
	}
}
