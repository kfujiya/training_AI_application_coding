package com.quizarena;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {
    @Test
    void returnsUpStatus() {
        Map<String, String> response = new HealthController().health();
        assertThat(response).containsEntry("status", "UP").containsEntry("service", "quiz-arena-backend");
    }
}
