package com.example.task_manager.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class SlackOAuthStateService {
    private static final String SESSION_KEY = "slack_oauth_state";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(HttpSession session, String userEmail) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, new OAuthState(value, userEmail, Instant.now().plus(STATE_TTL)));
        return value;
    }

    public OAuthState consume(HttpSession session, String returnedState) {
        Object stored = session.getAttribute(SESSION_KEY);
        session.removeAttribute(SESSION_KEY);
        if (!(stored instanceof OAuthState expected) || returnedState == null) return null;
        boolean same = MessageDigest.isEqual(expected.value().getBytes(StandardCharsets.UTF_8),
                returnedState.getBytes(StandardCharsets.UTF_8));
        if (!same || expected.expiresAt().isBefore(Instant.now())) return null;
        return expected;
    }

    public record OAuthState(String value, String userEmail, Instant expiresAt) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
