package sample.readinglog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

final class Database {
    private Database() {
    }

    static Connection connect() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC driver is not available", e);
        }
        String url = env("DB_URL", "jdbc:postgresql://127.0.0.1:5432/postgres");
        String user = env("DB_USER", "book_test_user");
        String password = env("DB_PASSWORD", "book_test_password_2026");
        return DriverManager.getConnection(url, user, password);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
