package com.example.weather.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import jakarta.annotation.PostConstruct;

@Configuration
@DependsOn("dataSource")
public class DatabaseConfiguration {
    private final DataSource dataSource;
    private final String url;
    private final String username;
    private final String password;

    public DatabaseConfiguration(DataSource dataSource,
                                 @Value("${spring.datasource.url}") String url,
                                 @Value("${spring.datasource.username}") String username,
                                 @Value("${spring.datasource.password}") String password) {
        this.dataSource = dataSource;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    void validate() {
        if (!url.matches("^jdbc:postgresql://127\\.0\\.0\\.1:5434/weather_db(?:[?].*)?$")) {
            throw new IllegalStateException("DB_URL must target PostgreSQL 127.0.0.1:5434/weather_db; refusing another database");
        }
        if (!"weather_app".equals(username)) {
            throw new IllegalStateException("DB_USERNAME must be weather_app");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("DB_PASSWORD is required; set it without committing the value");
        }
    }
}
