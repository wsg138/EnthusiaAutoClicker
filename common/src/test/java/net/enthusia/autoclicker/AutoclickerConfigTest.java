package net.enthusia.autoclicker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
