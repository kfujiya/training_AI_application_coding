package com.example.weather.repository;

import com.example.weather.domain.WeatherRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WeatherRepository {
    private final JdbcTemplate jdbc;
    public WeatherRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void upsert(long locationId, OffsetDateTime fetchedAt, WeatherRecord w) {
        jdbc.update("""
                INSERT INTO weather_records(location_id, fetched_at, forecast_at, weather_code,
                    temperature_c, apparent_temperature_c, precipitation_mm, precipitation_probability,
                    relative_humidity, wind_speed_kmh)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (location_id, forecast_at) DO UPDATE SET
                    fetched_at = EXCLUDED.fetched_at, weather_code = EXCLUDED.weather_code,
                    temperature_c = EXCLUDED.temperature_c, apparent_temperature_c = EXCLUDED.apparent_temperature_c,
                    precipitation_mm = EXCLUDED.precipitation_mm, precipitation_probability = EXCLUDED.precipitation_probability,
                    relative_humidity = EXCLUDED.relative_humidity, wind_speed_kmh = EXCLUDED.wind_speed_kmh
                """, locationId, fetchedAt, w.forecastAt(), w.weatherCode(), w.temperatureC(),
                w.apparentTemperatureC(), w.precipitationMm(), w.precipitationProbability(),
                w.relativeHumidity(), w.windSpeedKmh());
    }

    public List<WeatherRecord> find(long locationId, OffsetDateTime from, OffsetDateTime to) {
        StringBuilder sql = new StringBuilder("""
                SELECT w.*, l.name AS location_name FROM weather_records w
                JOIN locations l ON l.location_id = w.location_id WHERE w.location_id = ?
                """);
        var args = new java.util.ArrayList<Object>(); args.add(locationId);
        if (from != null) { sql.append(" AND w.forecast_at >= ?"); args.add(from); }
        if (to != null) { sql.append(" AND w.forecast_at < ?"); args.add(to); }
        sql.append(" ORDER BY w.forecast_at DESC");
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public List<WeatherRecord> findLatest(long locationId, int limit) {
        return jdbc.query("""
                SELECT w.*, l.name AS location_name FROM weather_records w
                JOIN locations l ON l.location_id = w.location_id
                WHERE w.location_id = ? ORDER BY w.forecast_at LIMIT ?
                """, this::map, locationId, limit);
    }

    private WeatherRecord map(ResultSet rs, int row) throws SQLException {
        return new WeatherRecord(rs.getLong("weather_record_id"), rs.getLong("location_id"),
                rs.getString("location_name"), rs.getObject("fetched_at", OffsetDateTime.class),
                rs.getObject("forecast_at", OffsetDateTime.class), rs.getInt("weather_code"),
                rs.getBigDecimal("temperature_c"), rs.getBigDecimal("apparent_temperature_c"),
                rs.getBigDecimal("precipitation_mm"), rs.getInt("precipitation_probability"),
                rs.getInt("relative_humidity"), rs.getBigDecimal("wind_speed_kmh"),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}
