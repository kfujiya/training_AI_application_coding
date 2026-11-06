package com.example.dollclothingcatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ColorMagicClientTests {
	@Test
	void searchesWithEncodedQueryAndMapsPaletteFields() throws Exception {
		String payload = """
				[
				  {"id":"p-1","text":"Mint Vintage","colors":["#36bfb0","#FFFFFF"],
				   "tags":["mint","vintage"],"likesCount":12}
				]
				""";
		AtomicReference<String> rawQuery = new AtomicReference<>();
		HttpServer server = startServer(200, payload, rawQuery);
		try {
			var client = clientFor(server);
			var palettes = client.search("dark blue & mint");

			assertEquals("q=dark blue & mint",
					URLDecoder.decode(rawQuery.get(), StandardCharsets.UTF_8));
			assertEquals(1, palettes.size());
			assertEquals("p-1", palettes.get(0).id());
			assertEquals("Mint Vintage", palettes.get(0).text());
			assertEquals("#36BFB0", palettes.get(0).colors().get(0));
			assertEquals("vintage", palettes.get(0).tags().get(1));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void reportsNonSuccessApiResponses() throws Exception {
		HttpServer server = startServer(503, "service unavailable", new AtomicReference<>());
		try {
			var client = clientFor(server);
			assertThrows(ColorMagicClient.ColorMagicException.class, () -> client.search("mint"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void rejectsMalformedApiResponseShapes() {
		var client = new ColorMagicClient(HttpClient.newHttpClient(), new ObjectMapper(),
				"https://colormagic.app/api");
		assertThrows(ColorMagicClient.ColorMagicException.class,
				() -> client.parsePalettes("{\"results\":[]}"));
		assertThrows(ColorMagicClient.ColorMagicException.class,
				() -> client.parsePalettes("""
						[{"id":"bad","text":"Invalid","colors":["not-a-hex"],"tags":[]}]
						"""));
	}

	private static ColorMagicClient clientFor(HttpServer server) {
		String apiUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api";
		return new ColorMagicClient(HttpClient.newHttpClient(), new ObjectMapper(), apiUrl);
	}

	private static HttpServer startServer(int status, String body,
			AtomicReference<String> rawQuery) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/palette/search", exchange -> {
			rawQuery.set(exchange.getRequestURI().getRawQuery());
			byte[] response = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, response.length);
			try (var output = exchange.getResponseBody()) {
				output.write(response);
			}
		});
		server.start();
		return server;
	}
}
