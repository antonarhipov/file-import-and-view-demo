package org.example.weather;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherReading(String name, LocalDateTime datetime, BigDecimal temperature) {
}
