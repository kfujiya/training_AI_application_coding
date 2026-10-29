package com.example.weather.repository;

import com.example.weather.domain.Location;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class LocationRepository {
    private final JdbcTemplate jdbc;
    public LocationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Location> findAll() {
        return jdbc.query("SELECT * FROM locations ORDER BY name", this::map);
    }

    public Location findById(long id) {
        return jdbc.query("SELECT * FROM locations WHERE location_id = ?", this::map, id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("地点が見つかりません。"));
    }

    public long insert(String name, String country, String countryCode, BigDecimal latitude, BigDecimal longitude) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO locations(name, country, country_code, latitude, longitude)
                    VALUES (?, ?, ?, ?, ?)
                    """, new String[] {"location_id"});
            ps.setString(1, name); ps.setString(2, country); ps.setString(3, countryCode);
            ps.setBigDecimal(4, latitude); ps.setBigDecimal(5, longitude); return ps;
        }, holder);
        return holder.getKey().longValue();
    }

    public void delete(long id) { jdbc.update("DELETE FROM locations WHERE location_id = ?", id); }

    private Location map(ResultSet rs, int row) throws SQLException {
        return new Location(rs.getLong("location_id"), rs.getString("name"), rs.getString("country"),
                rs.getString("country_code"), rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class));
    }
}
