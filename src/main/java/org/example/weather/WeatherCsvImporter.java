package org.example.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class WeatherCsvImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WeatherCsvImporter.class);
    private static final int SUCCESS_NO_INFO = -2;

    private final WeatherReadingRepository repository;
    private final Path file;
    private final int batchSize;

    public WeatherCsvImporter(
            WeatherReadingRepository repository,
            @Value("${weather.import.file}") String file,
            @Value("${weather.import.batch-size:1000}") int batchSize) {
        this.repository = repository;
        this.file = Path.of(file);
        this.batchSize = batchSize;
    }

    @Override
    public void run(String... args) {
        if (!Files.isRegularFile(file)) {
            log.error("Weather import skipped: CSV file does not exist: {}", file.toAbsolutePath());
            return;
        }

        ImportSummary summary = new ImportSummary();
        List<WeatherReading> batch = new ArrayList<>(batchSize);
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            reader.readLine();
            String line;
            long lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                WeatherReading reading = parse(line, lineNumber);
                if (reading == null) {
                    summary.invalid++;
                    continue;
                }
                batch.add(reading);
                if (batch.size() == batchSize) {
                    saveBatch(batch, summary);
                }
            }
            saveBatch(batch, summary);
            log.info("Weather import finished: inserted={}, duplicates skipped={}, invalid rows skipped={}",
                    summary.inserted, summary.duplicates, summary.invalid);
        }
        catch (IOException exception) {
            log.error("Weather import failed while reading {}", file.toAbsolutePath(), exception);
        }
    }

    private WeatherReading parse(String line, long lineNumber) {
        try {
            List<String> fields = firstThreeCsvFields(line);
            String name = fields.get(0).trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("name is blank");
            }
            return new WeatherReading(name, LocalDateTime.parse(fields.get(1).trim()),
                    new BigDecimal(fields.get(2).trim()));
        }
        catch (IllegalArgumentException | DateTimeParseException exception) {
            log.warn("Skipping invalid weather CSV row {}: {}", lineNumber, exception.getMessage());
            return null;
        }
    }

    private void saveBatch(List<WeatherReading> batch, ImportSummary summary) {
        if (batch.isEmpty()) {
            return;
        }
        int[] updateCounts = repository.insertIgnoringDuplicates(batch);
        for (int updateCount : updateCounts) {
            if (updateCount == 0) {
                summary.duplicates++;
            }
            else if (updateCount == SUCCESS_NO_INFO) {
                summary.inserted++;
            }
            else {
                summary.inserted += updateCount;
            }
        }
        batch.clear();
    }

    static List<String> firstThreeCsvFields(String line) {
        List<String> fields = new ArrayList<>(3);
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (quoted) {
                if (character == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        field.append(character);
                        index++;
                    }
                    else {
                        quoted = false;
                    }
                }
                else {
                    field.append(character);
                }
            }
            else if (character == ',') {
                fields.add(field.toString());
                if (fields.size() == 3) {
                    return fields;
                }
                field.setLength(0);
            }
            else if (character == '"' && field.isEmpty()) {
                quoted = true;
            }
            else {
                field.append(character);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException("unterminated quoted field");
        }
        fields.add(field.toString());
        if (fields.size() < 3) {
            throw new IllegalArgumentException("expected name, datetime, and temp columns");
        }
        return fields;
    }

    private static final class ImportSummary {
        private long inserted;
        private long duplicates;
        private long invalid;
    }
}
