package com.example.task_manager.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SlackOAuthStateServiceTest {
    private final SlackOAuthStateService service = new SlackOAuthStateService();

    @Test
    void issuedStateIsConsumedOnlyOnce() {
        HttpSession session = new MockHttpSession();
        String state = service.issue(session, "user@example.com");

        assertNotNull(service.consume(session, state));
        assertNull(service.consume(session, state));
    }

    @Test
    void mismatchedAndExpiredStateAreRejectedAndRemoved() {
        MockHttpSession session = new MockHttpSession();
        String state = service.issue(session, "user@example.com");
        assertNull(service.consume(session, state + "invalid"));
        assertNull(service.consume(session, state));

        session.setAttribute("slack_oauth_state",
                new SlackOAuthStateService.OAuthState("expired", "user@example.com", Instant.now().minusSeconds(1)));
        assertNull(service.consume(session, "expired"));
    }
}
