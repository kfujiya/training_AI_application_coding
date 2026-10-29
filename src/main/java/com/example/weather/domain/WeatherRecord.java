package com.example.weather.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WeatherRecord(Long id, Long locationId, String locationName,
                            OffsetDateTime fetchedAt, OffsetDateTime forecastAt,
                            Integer weatherCode, BigDecimal temperatureC,
                            BigDecimal apparentTemperatureC, BigDecimal precipitationMm,
                            Integer precipitationProbability, Integer relativeHumidity,
                            BigDecimal windSpeedKmh, OffsetDateTime createdAt) { }
