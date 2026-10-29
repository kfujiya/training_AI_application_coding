package com.example.weather.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Location(Long id, String name, String country, String countryCode,
                       BigDecimal latitude, BigDecimal longitude,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
