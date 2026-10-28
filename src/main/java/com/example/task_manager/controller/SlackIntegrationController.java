package com.example.task_manager.controller;

import com.example.task_manager.dto.SlackOAuthAccessResponse;
import com.example.task_manager.service.SlackIntegrationService;
import com.example.task_manager.service.SlackIntegrationService.SlackAccountAlreadyConnectedException;
import com.example.task_manager.service.SlackIntegrationService.SlackOAuthResponseException;
import com.example.task_manager.service.SlackOAuthClient;
import com.example.task_manager.service.SlackOAuthService;
import com.example.task_manager.service.SlackOAuthService.SlackOAuthConfigurationException;
import com.example.task_manager.service.SlackOAuthStateService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequestMapping("/settings/slack")
public class SlackIntegrationController {
    private final SlackOAuthService oauthService;
    private final SlackOAuthClient oauthClient;
    private final SlackOAuthStateService stateService;
    private final SlackIntegrationService integrationService;

    public SlackIntegrationController(SlackOAuthService oauthService,
                                      SlackOAuthClient oauthClient,
                                      SlackOAuthStateService stateService,
                                      SlackIntegrationService integrationService) {
        this.oauthService = oauthService;
        this.oauthClient = oauthClient;
        this.stateService = stateService;
        this.integrationService = integrationService;
    }

    @GetMapping
    public String settings(Authentication authentication, Model model) {
        model.addAttribute("slackConnection",
                integrationService.findConnection(authentication.getName()).orElse(null));
        return "settings/slack";
    }

    @GetMapping("/connect")
    public String connect(Authentication authentication, HttpSession session) {
        try {
            String state = stateService.issue(session, authentication.getName());
            return "redirect:" + oauthService.authorizationUrl(state);
        } catch (SlackOAuthConfigurationException e) {
            return "redirect:/settings/slack?error=config";
        }
    }

    @GetMapping("/callback")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error,
                           Authentication authentication,
                           HttpSession session) {
        SlackOAuthStateService.OAuthState oauthState = stateService.consume(session, state);
        if (oauthState == null || !oauthState.userEmail().equals(authentication.getName())) {
            return "redirect:/settings/slack?error=state";
        }
        if (error != null || code == null || code.isBlank()) {
            return "redirect:/settings/slack?error=denied";
        }
        try {
            SlackOAuthAccessResponse response = oauthClient.exchangeCode(code);
            integrationService.connect(authentication.getName(), response);
            return "redirect:/settings/slack?connected";
        } catch (SlackAccountAlreadyConnectedException e) {
            return "redirect:/settings/slack?error=duplicate";
        } catch (SlackOAuthResponseException | IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "redirect:/settings/slack?error=oauth";
        } catch (RuntimeException e) {
            return "redirect:/settings/slack?error=oauth";
        }
    }

    @PostMapping("/disconnect")
    public String disconnect(Authentication authentication) {
        integrationService.disconnect(authentication.getName());
        return "redirect:/settings/slack?disconnected";
    }
}
