CREATE TABLE locations (
    location_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    country VARCHAR(160),
    country_code CHAR(2),
    latitude NUMERIC(9, 6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(9, 6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_locations_name_coordinates UNIQUE (name, latitude, longitude)
);

CREATE TABLE weather_records (
    weather_record_id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES locations(location_id) ON DELETE RESTRICT,
    fetched_at TIMESTAMPTZ NOT NULL,
    forecast_at TIMESTAMPTZ NOT NULL,
    weather_code INTEGER NOT NULL,
    temperature_c NUMERIC(6, 2) NOT NULL,
    apparent_temperature_c NUMERIC(6, 2) NOT NULL,
    precipitation_mm NUMERIC(8, 2) NOT NULL CHECK (precipitation_mm >= 0),
    precipitation_probability INTEGER NOT NULL CHECK (precipitation_probability BETWEEN 0 AND 100),
    relative_humidity INTEGER NOT NULL CHECK (relative_humidity BETWEEN 0 AND 100),
    wind_speed_kmh NUMERIC(8, 2) NOT NULL CHECK (wind_speed_kmh >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weather_location_forecast UNIQUE (location_id, forecast_at)
);

CREATE INDEX idx_weather_records_location_forecast
    ON weather_records (location_id, forecast_at DESC);

CREATE INDEX idx_weather_records_fetched_at
    ON weather_records (fetched_at DESC);
