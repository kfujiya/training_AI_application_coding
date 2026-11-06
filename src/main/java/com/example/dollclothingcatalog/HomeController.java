package com.example.dollclothingcatalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
	private final CatalogService catalog;

	public HomeController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/")
	public String home(Model model) {
		var dolls = catalog.dolls();
		model.addAttribute("dolls", dolls);
		model.addAttribute("dollCount", dolls.size());
		model.addAttribute("clothes", catalog.clothingCount());
		model.addAttribute("pageTitle", "うちの子たち");
		return "home";
	}
}
