package com.example.weather.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateLocationRequest(@NotBlank String name, String country, String countryCode,
                                    @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                                    @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) { }
