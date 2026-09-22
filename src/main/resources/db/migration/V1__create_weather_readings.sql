CREATE TABLE weather_readings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    observed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    temperature_c NUMERIC(8, 3) NOT NULL,
    CONSTRAINT uk_weather_readings_name_observed_at UNIQUE (name, observed_at)
);

CREATE INDEX idx_weather_readings_observed_at_name
    ON weather_readings (observed_at DESC, name ASC);
