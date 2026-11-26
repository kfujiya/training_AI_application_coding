package com.quizarena.quiz.client;

import java.time.Duration;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenTriviaDbClient {
    private static final long MIN_REQUEST_INTERVAL_NANOS = Duration.ofSeconds(5).toNanos();
    private final RestClient restClient;
    private long lastRequestNanos;

    public OpenTriviaDbClient() {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl("https://opentdb.com")
                .requestFactory(requestFactory)
                .build();
    }

    public CategoryApiResponse fetchCategories() {
        waitForRateLimit();
        return restClient.get()
                .uri("/api_category.php")
                .retrieve()
                .body(CategoryApiResponse.class);
    }

    public QuestionApiResponse fetchQuestions(int amount, Integer category, String difficulty) {
        waitForRateLimit();
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api.php")
                            .queryParam("amount", amount)
                            .queryParam("type", "multiple");
                    if (category != null) uriBuilder.queryParam("category", category);
                    if (difficulty != null && !difficulty.isBlank()) uriBuilder.queryParam("difficulty", difficulty);
                    return uriBuilder.build();
                })
                .retrieve()
                .body(QuestionApiResponse.class);
    }

    private synchronized void waitForRateLimit() {
        long remaining = MIN_REQUEST_INTERVAL_NANOS - (System.nanoTime() - lastRequestNanos);
        if (lastRequestNanos != 0 && remaining > 0) {
            try {
                Thread.sleep(Duration.ofNanos(remaining).toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Open Trivia DB request was interrupted", exception);
            }
        }
        lastRequestNanos = System.nanoTime();
    }

    public record CategoryApiResponse(@JsonProperty("trivia_categories") List<CategoryItem> triviaCategories) {
    }

    public record CategoryItem(int id, String name) {
    }

    public record QuestionApiResponse(@JsonProperty("response_code") int responseCode, List<QuestionItem> results) {
    }

    public record QuestionItem(
            String category,
            String type,
            String difficulty,
            @JsonProperty("question") String question,
            @JsonProperty("correct_answer") String correctAnswer,
            @JsonProperty("incorrect_answers") List<String> incorrectAnswers) {
    }
}
