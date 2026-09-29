package com.example.task_manager.controller;

import com.example.task_manager.form.RegistrationForm;
import com.example.task_manager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserService userService;
    public AuthController(UserService userService) { this.userService = userService; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/register") public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }
    @PostMapping("/register")
    public String register(@Valid RegistrationForm form, BindingResult result, Model model) {
        if (result.hasErrors()) return "register";
        try { userService.register(form); }
        catch (IllegalArgumentException e) { model.addAttribute("registrationError", e.getMessage()); return "register"; }
        return "redirect:/login?registered";
    }
}
