package com.example.weather.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class DatabaseConfigurationTest {
    @Test
    void refusesNonWeatherDatabasePort() {
        var config = new DatabaseConfiguration(mock(DataSource.class), "jdbc:postgresql://127.0.0.1:5432/weather_db", "weather_app", "secret");
        assertThrows(IllegalStateException.class, config::validate);
    }

    @Test
    void refusesMissingPassword() {
        var config = new DatabaseConfiguration(mock(DataSource.class), "jdbc:postgresql://127.0.0.1:5434/weather_db", "weather_app", "");
        assertThrows(IllegalStateException.class, config::validate);
    }
}
