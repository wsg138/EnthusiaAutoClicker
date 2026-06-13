package net.enthusia.autoclicker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DurationParserTest {
    @Test
    void parsesTicksMillisecondsAndSeconds() {
        assertEquals(1_000L, DurationParser.parseMillis("20t"));
        assertEquals(625L, DurationParser.parseMillis("12.5t"));
        assertEquals(1_250L, DurationParser.parseMillis("1250ms"));
        assertEquals(625L, DurationParser.parseMillis("0.625s"));
        assertEquals(2_000L, DurationParser.parseMillis("2s"));
    }

    @Test
    void rejectsIntervalsFasterThanTwelveAndAHalfTicks() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseIntervalMillis("12.48t"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseIntervalMillis("624ms"));
    }

    @Test
    void parsesAndFormatsTickOnlyFields() {
        assertEquals(625L, DurationParser.parseIntervalTicks("12.5"));
        assertEquals(0L, DurationParser.parseOptionalDurationTicks("0"));
        assertEquals("25", DurationParser.formatTicks(1_250L));
        assertEquals("12.5", DurationParser.formatTicks(625L));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseIntervalTicks("12.48"));
    }
}
