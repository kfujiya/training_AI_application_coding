package com.example.task_manager.service;

import com.example.task_manager.dto.SlackChatPostMessageResponse;
import com.example.task_manager.dto.SlackConversationsOpenResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class HttpSlackWebApiClient implements SlackWebApiClient {
    private static final String API_BASE = "https://slack.com/api/";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;

    public HttpSlackWebApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String openDirectMessage(String botToken, String slackUserId) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(new ConversationsOpenRequest(slackUserId));
        HttpResponse<String> response = send("conversations.open", botToken, body);
        SlackConversationsOpenResponse result = objectMapper.readValue(response.body(), SlackConversationsOpenResponse.class);
        if (!result.isOk() || result.getChannel() == null || result.getChannel().getId() == null
                || result.getChannel().getId().isBlank()) {
            throw new SlackWebApiException(result.getError());
        }
        return result.getChannel().getId();
    }

    @Override
    public void postMessage(String botToken, String channelId, String text) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(new ChatPostMessageRequest(channelId, text));
        HttpResponse<String> response = send("chat.postMessage", botToken, body);
        SlackChatPostMessageResponse result = objectMapper.readValue(response.body(), SlackChatPostMessageResponse.class);
        if (!result.isOk()) throw new SlackWebApiException(result.getError());
    }

    private HttpResponse<String> send(String method, String botToken, String body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + method))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + botToken)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SlackWebApiException("http_" + response.statusCode());
        }
        return response;
    }

    private record ConversationsOpenRequest(String users) { }
    private record ChatPostMessageRequest(String channel, String text) { }

    public static class SlackWebApiException extends RuntimeException {
        private final String errorCode;

        public SlackWebApiException(String errorCode) {
            this.errorCode = errorCode == null || errorCode.isBlank() ? "unknown" : errorCode;
        }

        public String getErrorCode() { return errorCode; }
    }
}
