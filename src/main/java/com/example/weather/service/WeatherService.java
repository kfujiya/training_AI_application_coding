package com.example.weather.service;

import com.example.weather.domain.Location;
import com.example.weather.domain.WeatherRecord;
import com.example.weather.dto.LocationSearchResult;
import com.example.weather.repository.LocationRepository;
import com.example.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeatherService {
    private final OpenMeteoClient api; private final LocationRepository locations; private final WeatherRepository weather;
    public WeatherService(OpenMeteoClient api, LocationRepository locations, WeatherRepository weather) {
        this.api = api; this.locations = locations; this.weather = weather;
    }
    public List<LocationSearchResult> search(String name) {
        if (name == null || name.trim().length() < 2) throw new IllegalArgumentException("都市名は2文字以上で入力してください。");
        return api.search(name.trim());
    }
    public List<Location> locations() { return locations.findAll(); }
    public Location location(long id) { return locations.findById(id); }
    public long add(String name, String country, String countryCode, BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) throw new IllegalArgumentException("緯度・経度は必須です。");
        return locations.insert(name, country, countryCode, lat, lon);
    }
    public void delete(long id) { locations.delete(id); }

    @Transactional
    public List<WeatherRecord> refresh(long locationId) {
        Location l = location(locationId); var forecast = api.forecast(l.latitude(), l.longitude());
        var h = forecast.hourly(); int size = h.time().size(); OffsetDateTime fetched = OffsetDateTime.now(ZoneOffset.UTC);
        if (java.util.stream.Stream.of(h.weather_code(), h.temperature_2m(), h.apparent_temperature(), h.precipitation(),
                h.precipitation_probability(), h.relative_humidity_2m(), h.wind_speed_10m()).anyMatch(v -> v == null || v.size() != size))
            throw new IllegalStateException("天気APIの必須項目が不足しているため保存しませんでした。");
        for (int i = 0; i < size; i++) {
            var record = new WeatherRecord(null, locationId, l.name(), fetched,
                    java.time.LocalDateTime.parse(h.time().get(i)).atOffset(java.time.ZoneOffset.ofHours(9)),
                    h.weather_code().get(i), h.temperature_2m().get(i), h.apparent_temperature().get(i),
                    h.precipitation().get(i), h.precipitation_probability().get(i), h.relative_humidity_2m().get(i),
                    h.wind_speed_10m().get(i), null);
            weather.upsert(locationId, fetched, record);
        }
        return weather.findLatest(locationId, 24);
    }
    public List<WeatherRecord> history(long id, OffsetDateTime from, OffsetDateTime to) { location(id); return weather.find(id, from, to); }
    public List<WeatherRecord> latest(long id) { location(id); return weather.findLatest(id, 24); }
}
