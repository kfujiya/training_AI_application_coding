package com.example.weather.dto;

import com.example.weather.domain.WeatherRecord;

public record WeatherResponse(WeatherRecord current, java.util.List<WeatherRecord> forecast) { }
