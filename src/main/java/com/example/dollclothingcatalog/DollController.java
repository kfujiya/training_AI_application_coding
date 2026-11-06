package com.example.dollclothingcatalog;

import static com.example.dollclothingcatalog.CatalogForms.DollForm;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

@Controller
public class DollController {
	private final CatalogService catalog;

	public DollController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/dolls")
	public String list(Model model) {
		model.addAttribute("dolls", catalog.dolls());
		model.addAttribute("pageTitle", "ドール一覧");
		return "dolls/list";
	}

	@GetMapping("/dolls/new")
	public String createForm(Model model) {
		model.addAttribute("dollForm", new DollForm());
		model.addAttribute("formTitle", "ドールをお迎え");
		return "dolls/form";
	}

	@PostMapping("/dolls")
	public String create(@Valid @ModelAttribute("dollForm") DollForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("formTitle", "ドールをお迎え");
			return "dolls/form";
		}
		long id = catalog.saveDoll(null, form);
		redirect.addFlashAttribute("successMessage", "ドールを登録しました。");
		return "redirect:/dolls/" + id;
	}

	@GetMapping("/dolls/{id}")
	public String detail(@PathVariable long id, Model model) {
		model.addAttribute("doll", catalog.doll(id));
		model.addAttribute("clothes", catalog.clothingForDoll(id));
		model.addAttribute("pageTitle", "ドールプロフィール");
		return "dolls/detail";
	}

	@GetMapping("/dolls/{id}/edit")
	public String editForm(@PathVariable long id, Model model) {
		var doll = catalog.doll(id);
		DollForm form = new DollForm();
		form.setName(doll.name());
		form.setMaker(doll.maker());
		form.setBodyName(doll.bodyName());
		form.setBodySize(doll.bodySize());
		form.setWigSize(doll.wigSize());
		form.setEyeSize(doll.eyeSize());
		form.setNotes(doll.notes());
		model.addAttribute("dollForm", form);
		model.addAttribute("dollId", id);
		model.addAttribute("formTitle", "プロフィールを編集");
		return "dolls/form";
	}

	@PostMapping("/dolls/{id}")
	public String update(@PathVariable long id, @Valid @ModelAttribute("dollForm") DollForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("dollId", id);
			model.addAttribute("formTitle", "プロフィールを編集");
			return "dolls/form";
		}
		catalog.saveDoll(id, form);
		redirect.addFlashAttribute("successMessage", "プロフィールを更新しました。");
		return "redirect:/dolls/" + id;
	}

	@PostMapping("/dolls/{id}/delete")
	public String delete(@PathVariable long id, RedirectAttributes redirect) {
		catalog.deleteDoll(id);
		redirect.addFlashAttribute("successMessage", "ドールを削除しました。");
		return "redirect:/dolls";
	}
}
