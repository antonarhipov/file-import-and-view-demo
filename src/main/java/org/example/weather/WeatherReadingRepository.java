package org.example.weather;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WeatherReadingRepository {

    private static final String INSERT = """
            INSERT INTO weather_readings (name, observed_at, temperature_c)
            VALUES (?, ?, ?)
            ON CONFLICT (name, observed_at) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public WeatherReadingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int[] insertIgnoringDuplicates(List<WeatherReading> readings) {
        return jdbcTemplate.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement statement, int index) throws SQLException {
                WeatherReading reading = readings.get(index);
                statement.setString(1, reading.name());
                statement.setObject(2, reading.datetime());
                statement.setBigDecimal(3, reading.temperature());
            }

            @Override
            public int getBatchSize() {
                return readings.size();
            }
        });
    }

    public long count() {
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM weather_readings", Long.class);
        return total == null ? 0 : total;
    }

    public List<WeatherReading> findPage(int limit, long offset) {
        return jdbcTemplate.query("""
                SELECT name, observed_at, temperature_c
                FROM weather_readings
                ORDER BY observed_at DESC, name ASC
                LIMIT ? OFFSET ?
                """, this::mapReading, limit, offset);
    }

    private WeatherReading mapReading(ResultSet resultSet, int rowNumber) throws SQLException {
        return new WeatherReading(
                resultSet.getString("name"),
                resultSet.getObject("observed_at", java.time.LocalDateTime.class),
                resultSet.getBigDecimal("temperature_c"));
    }
}
