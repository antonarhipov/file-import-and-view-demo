package org.example.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WeatherCsvImporterTests {

    @Test
    void readsTheThreeImportedColumnsIncludingAQuotedName() {
        assertThat(WeatherCsvImporter.firstThreeCsvFields("\"Amsterdam, NL\",2000-01-01T00:00:00,4.8,ignored"))
                .containsExactly("Amsterdam, NL", "2000-01-01T00:00:00", "4.8");
    }

    @Test
    void rejectsARowMissingAnImportedColumn() {
        assertThatThrownBy(() -> WeatherCsvImporter.firstThreeCsvFields("Amsterdam,2000-01-01T00:00:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected name");
    }
}
