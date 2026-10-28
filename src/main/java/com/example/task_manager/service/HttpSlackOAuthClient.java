package com.example.task_manager.service;

import com.example.task_manager.dto.SlackOAuthAccessResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.net.URLEncoder;

@Component
public class HttpSlackOAuthClient implements SlackOAuthClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public HttpSlackOAuthClient(ObjectMapper objectMapper,
                                @Value("${slack.oauth.client-id:}") String clientId,
                                @Value("${slack.oauth.client-secret:}") String clientSecret,
                                @Value("${slack.oauth.redirect-uri:}") String redirectUri) {
        this.objectMapper = objectMapper;
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.redirectUri = redirectUri == null ? "" : redirectUri.trim();
    }

    @Override
    public SlackOAuthAccessResponse exchangeCode(String code) throws IOException, InterruptedException {
        if (clientId.isBlank() || clientSecret.isBlank() || redirectUri.isBlank()) {
            throw new IOException("Slack OAuth settings are not configured");
        }
        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String body = "code=" + encode(code) + "&redirect_uri=" + encode(redirectUri);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://slack.com/api/oauth.v2.access"))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Slack OAuth returned HTTP " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), SlackOAuthAccessResponse.class);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
