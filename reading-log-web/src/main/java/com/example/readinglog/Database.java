package com.example.readinglog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String URL = getSetting("READING_LOG_DB_URL", "jdbc:postgresql://localhost:5432/postgres");
    private static final String USER = getSetting("READING_LOG_DB_USER", "postgres");
    private static final String PASSWORD = getSetting("READING_LOG_DB_PASSWORD", "password");

    private Database() {
    }

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String getSetting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
