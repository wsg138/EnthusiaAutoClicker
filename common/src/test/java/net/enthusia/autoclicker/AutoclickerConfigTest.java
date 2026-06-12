package net.enthusia.autoclicker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutoclickerConfigTest {
    @TempDir
    Path tempDirectory;

    @Test
    void migratesLegacyOffModesToIndependentToggles() throws IOException {
        Path path = tempDirectory.resolve("config.properties");
        Files.writeString(path, """
            left-mode=click
            right-mode=off
            left-interval-ms=1000
            right-interval-ms=1000
            run-duration-ms=0
            """);

        AutoclickerConfig config = AutoclickerConfig.load(path);

        assertTrue(config.leftEnabled());
        assertFalse(config.rightEnabled());
        assertEquals(ActionMode.CLICK, config.leftMode());
        assertEquals(ActionMode.CLICK, config.rightMode());
    }

    @Test
    void savesAndLoadsExtras() {
        Path path = tempDirectory.resolve("config.properties");
        AutoclickerConfig config = AutoclickerConfig.load(path);
        config.setDurabilityGuard(true);
        config.setMinimumDurability(25);
        config.setArmorStandEating(true);
        config.setAutoRestock(true);
        config.setRestockAtCount(3);
        config.setStopWhenOutOfFood(true);
        config.save();

        AutoclickerConfig loaded = AutoclickerConfig.load(path);

        assertTrue(loaded.durabilityGuard());
        assertEquals(25, loaded.minimumDurability());
        assertTrue(loaded.armorStandEating());
        assertTrue(loaded.autoRestock());
        assertEquals(3, loaded.restockAtCount());
        assertTrue(loaded.stopWhenOutOfFood());
    }

    @Test
    void rejectsUnsafeExtrasRanges() {
        AutoclickerConfig config = AutoclickerConfig.load(tempDirectory.resolve("config.properties"));

        assertThrows(IllegalArgumentException.class, () -> config.setMinimumDurability(0));
        assertThrows(IllegalArgumentException.class, () -> config.setRestockAtCount(64));
    }

    @Test
    void acceptsTwelveAndAHalfTickIntervals() {
        AutoclickerConfig config = AutoclickerConfig.load(tempDirectory.resolve("config.properties"));

        config.setLeftIntervalMillis(625L);

        assertEquals(625L, config.leftIntervalMillis());
        assertThrows(IllegalArgumentException.class, () -> config.setLeftIntervalMillis(624L));
    }
}
