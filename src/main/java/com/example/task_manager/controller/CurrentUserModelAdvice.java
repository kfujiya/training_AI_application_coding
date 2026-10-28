package com.example.task_manager.controller;

import com.example.task_manager.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserModelAdvice {
    private final UserRepository userRepository;

    public CurrentUserModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addCurrentUserDisplayName(Authentication authentication, Model model) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) return;
        userRepository.findByEmail(authentication.getName())
                .map(user -> user.getDisplayName())
                .ifPresent(displayName -> model.addAttribute("currentUserDisplayName", displayName));
    }
}
