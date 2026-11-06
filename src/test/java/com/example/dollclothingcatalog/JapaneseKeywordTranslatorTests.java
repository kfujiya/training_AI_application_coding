package com.example.dollclothingcatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class JapaneseKeywordTranslatorTests {
	@Test
	void translatesJapaneseQueryWithDeepLAndEncodesRequest() throws Exception {
		AtomicReference<String> authorization = new AtomicReference<>();
		AtomicReference<String> body = new AtomicReference<>();
		HttpServer server = startServer(200,
				"{\"translations\":[{\"detected_source_language\":\"JA\",\"text\":\"mint\"}]}",
				authorization, body);
		try {
			var translator = translator(server, "test-secret");
			var result = translator.translateToEnglish("ミント");

			assertEquals("DeepL-Auth-Key test-secret", authorization.get());
			assertTrue(body.get().contains("text=%E3%83%9F%E3%83%B3%E3%83%88"));
			assertTrue(body.get().contains("source_lang=JA"));
			assertTrue(body.get().contains("target_lang=EN"));
			assertEquals("ミント", result.originalQuery());
			assertEquals("mint", result.translatedQuery());
			assertTrue(result.translated());
		} finally {
			server.stop(0);
		}
	}

	@Test
	void leavesEnglishQueryUnchangedWithoutApiKey() {
		var translator = new JapaneseKeywordTranslator(HttpClient.newHttpClient(),
				new ObjectMapper(), "", "https://api-free.deepl.com/v2/translate");
		var result = translator.translateToEnglish("vintage blue");
		assertEquals("vintage blue", result.translatedQuery());
		assertFalse(result.translated());
	}

	@Test
	void explainsMissingDeepLKeyForJapaneseQuery() {
		var translator = new JapaneseKeywordTranslator(HttpClient.newHttpClient(),
				new ObjectMapper(), "", "https://api-free.deepl.com/v2/translate");
		assertThrows(JapaneseKeywordTranslator.TranslationException.class,
				() -> translator.translateToEnglish("ミント"));
	}

	@Test
	void reportsDeepLTranslationErrors() throws Exception {
		HttpServer server = startServer(456, "{\"message\":\"quota exceeded\"}",
				new AtomicReference<>(), new AtomicReference<>());
		try {
			var translator = translator(server, "test-secret");
			assertThrows(JapaneseKeywordTranslator.TranslationException.class,
					() -> translator.translateToEnglish("ミント"));
		} finally {
			server.stop(0);
		}
	}

	private static JapaneseKeywordTranslator translator(HttpServer server, String apiKey) {
		String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v2/translate";
		return new JapaneseKeywordTranslator(HttpClient.newHttpClient(), new ObjectMapper(),
				apiKey, endpoint);
	}

	private static HttpServer startServer(int status, String response,
			AtomicReference<String> authorization, AtomicReference<String> requestBody)
			throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v2/translate", exchange -> {
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(),
					StandardCharsets.UTF_8));
			byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, responseBytes.length);
			try (var output = exchange.getResponseBody()) {
				output.write(responseBytes);
			}
		});
		server.start();
		return server;
	}
}
