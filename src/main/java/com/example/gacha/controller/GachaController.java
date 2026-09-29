package com.example.gacha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.gacha.service.GachaService;
import com.example.gacha.service.GachaUnavailableException;

@Controller
public class GachaController {

    private final GachaService gachaService;

    public GachaController(GachaService gachaService) {
        this.gachaService = gachaService;
    }

    @GetMapping("/")
    public String home(Model model) {
        gachaService.findLatestHistory().ifPresent(history -> model.addAttribute("latestHistory", history));
        return "index";
    }

    @PostMapping("/draw")
    public String draw(Model model) {
        try {
            model.addAttribute("result", gachaService.draw());
        } catch (GachaUnavailableException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
        }
        return "index";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("histories", gachaService.findHistories());
        return "history";
    }
}
