package com.example.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OpenMeteoClientTest {
    @Test
    void searchesLocationsFromGeocodingResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://geo.test/v1/search?name=Tokyo&count=10&language=ja&format=json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"results\":[{\"name\":\"Tokyo\",\"country\":\"Japan\",\"country_code\":\"JP\",\"latitude\":35.68,\"longitude\":139.69}]}"));

        var result = new OpenMeteoClient(builder.build(), "https://geo.test", "https://forecast.test").search("Tokyo");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Tokyo");
        server.verify();
    }

    @Test
    void encodesJapaneseSearchTerm() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://geo.test/v1/search?name=%E6%9D%B1%E4%BA%AC&count=10&language=ja&format=json"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"results\":[{\"name\":\"東京\",\"country\":\"日本\",\"country_code\":\"JP\",\"latitude\":35.68,\"longitude\":139.69}]}"));

        var result = new OpenMeteoClient(builder.build(), "https://geo.test", "https://forecast.test").search("東京");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("東京");
        server.verify();
    }

    @Test
    void reportsExternalApiFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://geo.test/v1/search?name=Tokyo&count=10&language=ja&format=json"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(RuntimeException.class, () -> new OpenMeteoClient(builder.build(), "https://geo.test", "https://forecast.test").search("Tokyo"));
        server.verify();
    }
}
