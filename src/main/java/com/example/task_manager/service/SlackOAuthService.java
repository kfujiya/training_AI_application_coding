package com.example.task_manager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SlackOAuthService {
    private static final String AUTHORIZE_URL = "https://slack.com/oauth/v2/authorize";
    private final String clientId;
    private final String redirectUri;

    public SlackOAuthService(@Value("${slack.oauth.client-id:}") String clientId,
                             @Value("${slack.oauth.redirect-uri:}") String redirectUri) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.redirectUri = redirectUri == null ? "" : redirectUri.trim();
    }

    public String authorizationUrl(String state) {
        if (clientId.isBlank() || redirectUri.isBlank()) {
            throw new SlackOAuthConfigurationException();
        }
        return AUTHORIZE_URL
                + "?client_id=" + encode(clientId)
                + "&scope=" + encode("chat:write,im:write")
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state);
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    public static class SlackOAuthConfigurationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
