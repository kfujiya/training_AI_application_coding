package com.example.dollclothingcatalog;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class JapaneseKeywordTranslator {
	private static final Logger logger = LoggerFactory.getLogger(JapaneseKeywordTranslator.class);
	private static final Pattern JAPANESE = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
	private static final int MAX_ATTEMPTS = 3;

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String authKey;
	private final URI endpoint;

	@Autowired
	public JapaneseKeywordTranslator(ObjectMapper objectMapper,
			@Value("${DEEPL_AUTH_KEY:}") String authKey,
			@Value("${deepl.api-url:https://api-free.deepl.com/v2/translate}") String apiUrl) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
				objectMapper, authKey, apiUrl);
	}

	JapaneseKeywordTranslator(HttpClient httpClient, ObjectMapper objectMapper,
			String authKey, String apiUrl) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.authKey = authKey == null ? "" : authKey.trim();
		this.endpoint = URI.create(apiUrl);
	}

	public Translation translateToEnglish(String query) {
		if (query == null || query.isBlank() || query.length() > 100) {
			throw new IllegalArgumentException("検索キーワードは1〜100文字で入力してください。");
		}
		String original = query.trim();
		if (!JAPANESE.matcher(original).find()) {
			return new Translation(original, original, false);
		}
		if (authKey.isBlank()) {
			throw new TranslationException(
					"日本語検索にはDeepL API Freeキーが必要です。DEEPL_AUTH_KEYを設定してください。");
		}

		String form = "text=" + encode(original) + "&source_lang=JA&target_lang=EN";
		HttpRequest request = HttpRequest.newBuilder(endpoint)
				.timeout(Duration.ofSeconds(5))
				.header("Authorization", "DeepL-Auth-Key " + authKey)
				.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(form))
				.build();
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				HttpResponse<String> response = httpClient.send(request,
						HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					String translated = parseTranslation(response.body());
					return new Translation(original, translated, true);
				}
				String traceId = response.headers().firstValue("X-Trace-ID").orElse("unavailable");
				logger.warn("DeepL returned HTTP status {} (trace ID {})",
						response.statusCode(), traceId);
				if ((response.statusCode() == 429 || response.statusCode() >= 500)
						&& attempt < MAX_ATTEMPTS) {
					pauseBeforeRetry(attempt);
					continue;
				}
				if (response.statusCode() == 456) {
					throw new TranslationException(
							"DeepL APIの利用上限に達しました。DeepLアカウントを確認してください。");
				}
				throw new TranslationException(
						"日本語キーワードを翻訳できませんでした。時間をおいて再度お試しください。");
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				logger.warn("DeepL translation was interrupted", exception);
				throw new TranslationException("翻訳が中断されました。もう一度お試しください。", exception);
			} catch (IOException exception) {
				logger.warn("DeepL translation request failed", exception);
				throw new TranslationException(
						"翻訳サービスに接続できませんでした。時間をおいて再度お試しください。", exception);
			}
		}
		throw new TranslationException("日本語キーワードを翻訳できませんでした。");
	}

	private String parseTranslation(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);
			JsonNode translations = root == null ? null : root.path("translations");
			if (translations == null || !translations.isArray() || translations.isEmpty()
					|| !translations.get(0).path("text").isTextual()) {
				throw new TranslationException("翻訳サービスの応答形式が想定と異なります。");
			}
			String translated = translations.get(0).path("text").asText().trim();
			if (translated.isBlank() || translated.length() > 100) {
				throw new TranslationException("翻訳結果が検索できる長さではありません。");
			}
			return translated;
		} catch (JacksonException exception) {
			logger.warn("DeepL returned invalid JSON", exception);
			throw new TranslationException("翻訳サービスの応答を読み取れませんでした。", exception);
		}
	}

	private static void pauseBeforeRetry(int attempt) {
		try {
			Thread.sleep(200L * attempt);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new TranslationException("翻訳が中断されました。もう一度お試しください。",
					exception);
		}
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public record Translation(String originalQuery, String translatedQuery, boolean translated) {
	}

	static final class TranslationException extends RuntimeException {
		TranslationException(String message) {
			super(message);
		}

		TranslationException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
