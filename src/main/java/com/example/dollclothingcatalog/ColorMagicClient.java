package com.example.dollclothingcatalog;

import static com.example.dollclothingcatalog.CatalogModels.ColorPalette;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ColorMagicClient {
	private static final Logger logger = LoggerFactory.getLogger(ColorMagicClient.class);
	private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String apiUrl;

	@Autowired
	public ColorMagicClient(ObjectMapper objectMapper,
			@org.springframework.beans.factory.annotation.Value("${colormagic.api-url:https://colormagic.app/api}")
			String apiUrl) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
				objectMapper, apiUrl);
	}

	ColorMagicClient(HttpClient httpClient, ObjectMapper objectMapper, String apiUrl) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.apiUrl = apiUrl;
	}

	public List<ColorPalette> search(String query) {
		if (query == null || query.isBlank() || query.length() > 100) {
			throw new IllegalArgumentException("検索キーワードは1〜100文字で入力してください。");
		}
		URI uri = UriComponentsBuilder.fromUriString(apiUrl)
				.path("/palette/search")
				.queryParam("q", query.trim())
				.build()
				.encode()
				.toUri();
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(5))
				.header("Accept", "application/json")
				.GET()
				.build();
		try {
			HttpResponse<String> response = httpClient.send(request,
					HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				logger.warn("ColorMagic returned HTTP status {}", response.statusCode());
				throw new ColorMagicException("配色サービスから正常な応答がありませんでした。");
			}
			return parsePalettes(response.body());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			logger.warn("ColorMagic request was interrupted", exception);
			throw new ColorMagicException("配色検索が中断されました。もう一度お試しください。", exception);
		} catch (IOException exception) {
			logger.warn("ColorMagic request failed", exception);
			throw new ColorMagicException("配色サービスに接続できませんでした。時間をおいて再度お試しください。",
					exception);
		}
	}

	List<ColorPalette> parsePalettes(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);
			if (root == null || !root.isArray()) {
				throw new ColorMagicException("配色サービスの応答形式が想定と異なります。");
			}
			List<ColorPalette> palettes = new ArrayList<>();
			for (JsonNode node : root) {
				ColorPalette palette = parsePalette(node);
				if (palette == null) {
					throw new ColorMagicException("配色サービスの応答形式が想定と異なります。");
				}
				palettes.add(palette);
			}
			return List.copyOf(palettes);
		} catch (JacksonException exception) {
			logger.warn("ColorMagic returned invalid JSON", exception);
			throw new ColorMagicException("配色サービスの応答を読み取れませんでした。", exception);
		}
	}

	private ColorPalette parsePalette(JsonNode node) {
		if (!node.isObject() || !node.path("id").isTextual()
				|| !node.path("text").isTextual() || !node.path("colors").isArray()) {
			return null;
		}
		String id = node.path("id").asText();
		String text = node.path("text").asText();
		if (id.isBlank() || id.length() > 100 || text.isBlank() || text.length() > 200) {
			return null;
		}
		List<String> colors = new ArrayList<>();
		for (JsonNode color : node.path("colors")) {
			if (!color.isTextual() || !HEX_COLOR.matcher(color.asText()).matches()) {
				return null;
			}
			colors.add(color.asText().toUpperCase(java.util.Locale.ROOT));
			if (colors.size() > 20) {
				return null;
			}
		}
		if (colors.isEmpty()) {
			return null;
		}
		List<String> tags = new ArrayList<>();
		JsonNode tagNodes = node.path("tags");
		if (tagNodes.isArray()) {
			for (JsonNode tag : tagNodes) {
				if (tag.isTextual() && !tag.asText().isBlank()
						&& tag.asText().length() <= 100 && tags.size() < 30) {
					tags.add(tag.asText());
				}
			}
		}
		return new ColorPalette(id, text, List.copyOf(colors), List.copyOf(tags));
	}

	static final class ColorMagicException extends RuntimeException {
		ColorMagicException(String message) {
			super(message);
		}

		ColorMagicException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
