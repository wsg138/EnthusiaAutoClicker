package net.enthusia.autoclicker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DurationParserTest {
    @Test
    void parsesTicksMillisecondsAndSeconds() {
        assertEquals(1_000L, DurationParser.parseMillis("20t"));
        assertEquals(1_250L, DurationParser.parseMillis("1250ms"));
        assertEquals(2_000L, DurationParser.parseMillis("2s"));
    }

    @Test
    void rejectsIntervalsFasterThanTwentyTicks() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseIntervalMillis("19t"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseIntervalMillis("999ms"));
    }
}
