package com.example.weather.controller;

import com.example.weather.domain.Location;
import com.example.weather.domain.WeatherRecord;
import com.example.weather.dto.CreateLocationRequest;
import com.example.weather.dto.LocationSearchResult;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.service.WeatherService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WeatherController {
    private final WeatherService service;
    public WeatherController(WeatherService service) { this.service = service; }

    @GetMapping("/locations/search") public List<LocationSearchResult> search(@RequestParam String name) { return service.search(name); }
    @GetMapping("/locations") public List<Location> locations() { return service.locations(); }
    @PostMapping("/locations") @ResponseStatus(HttpStatus.CREATED)
    public Location add(@Valid @RequestBody CreateLocationRequest r) {
        return service.location(service.add(r.name(), r.country(), r.countryCode(), r.latitude(), r.longitude()));
    }
    @DeleteMapping("/locations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id) { service.delete(id); }

    @PostMapping("/locations/{id}/weather") public WeatherResponse refresh(@PathVariable long id) {
        List<WeatherRecord> records = service.refresh(id); return new WeatherResponse(records.isEmpty() ? null : records.get(0), records);
    }
    @GetMapping("/locations/{id}/weather") public List<WeatherRecord> latest(@PathVariable long id) { return service.latest(id); }
    @GetMapping("/weather/history") public List<WeatherRecord> history(@RequestParam long locationId,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        OffsetDateTime start = from == null ? null : from.atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        OffsetDateTime end = to == null ? null : to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        return service.history(locationId, start, end);
    }
}
