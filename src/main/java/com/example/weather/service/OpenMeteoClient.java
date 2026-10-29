package com.example.weather.service;

import com.example.weather.dto.LocationSearchResult;
import com.example.weather.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OpenMeteoClient {
    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);
    private final RestClient client;
    private final String geocodingUrl;
    private final String forecastUrl;

    public OpenMeteoClient(RestClient client, @Value("${open-meteo.geocoding-url}") String geocodingUrl,
                           @Value("${open-meteo.forecast-url}") String forecastUrl) {
        this.client = client; this.geocodingUrl = geocodingUrl; this.forecastUrl = forecastUrl;
    }

    public List<LocationSearchResult> search(String name) {
        try {
            var uri = UriComponentsBuilder.fromUriString(geocodingUrl + "/v1/search")
                    .queryParam("name", name).queryParam("count", 10).queryParam("language", "ja").queryParam("format", "json")
                    .encode().build().toUri();
            var response = client.get().uri(uri).retrieve().body(GeocodingResponse.class);
            return response == null || response.results() == null ? List.of() : response.results().stream()
                    .map(r -> new LocationSearchResult(r.name(), r.country(), r.countryCode(), r.latitude(), r.longitude())).toList();
        } catch (RuntimeException e) {
            log.warn("Open-Meteo地点検索に失敗しました（検索語長={}）", name == null ? 0 : name.length(), e);
            throw new ExternalApiException("地点検索に失敗しました。Open-Meteoとの通信を確認してください。", e);
        }
    }

    public ForecastResponse forecast(BigDecimal latitude, BigDecimal longitude) {
        try {
            var uri = UriComponentsBuilder.fromUriString(forecastUrl + "/v1/forecast")
                    .queryParam("latitude", latitude).queryParam("longitude", longitude)
                    .queryParam("hourly", "weather_code,temperature_2m,apparent_temperature,precipitation,precipitation_probability,relative_humidity_2m,wind_speed_10m")
                    .queryParam("forecast_days", 7).queryParam("timezone", "Asia/Tokyo").build().toUri();
            var response = client.get().uri(uri).retrieve().body(ForecastResponse.class);
            if (response == null || response.hourly() == null) throw new ExternalApiException("天気APIから空の応答を受信しました。");
            return response;
        } catch (ExternalApiException e) { throw e; }
        catch (RuntimeException e) { throw new ExternalApiException("天気情報の取得に失敗しました。Open-Meteoとの通信を確認してください。", e); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingResponse(List<GeocodingLocation> results) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingLocation(String name, String country, @JsonProperty("country_code") String countryCode,
                                    BigDecimal latitude, BigDecimal longitude) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastResponse(Hourly hourly) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(List<String> time, @JsonProperty("weather_code") List<Integer> weather_code,
                         @JsonProperty("temperature_2m") List<BigDecimal> temperature_2m,
                         @JsonProperty("apparent_temperature") List<BigDecimal> apparent_temperature,
                         List<BigDecimal> precipitation,
                         @JsonProperty("precipitation_probability") List<Integer> precipitation_probability,
                         @JsonProperty("relative_humidity_2m") List<Integer> relative_humidity_2m,
                         @JsonProperty("wind_speed_10m") List<BigDecimal> wind_speed_10m) { }
}
