package com.example.weather.dto;

import java.math.BigDecimal;

public record LocationSearchResult(String name, String country, String countryCode,
                                   BigDecimal latitude, BigDecimal longitude) { }
