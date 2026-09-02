package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DefaultTargetConfigurationTest {
    @Test
    void defaultFilterAllowsArmorStandsButStillDeniesPlayersAndVillagers() throws IOException {
        try (InputStream input = DefaultTargetConfigurationTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(input);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            List<String> deniedTypes = config.getStringList("target-filter.denied-types");

            assertFalse(deniedTypes.contains("ARMOR_STAND"));
            assertTrue(deniedTypes.contains("PLAYER"));
            assertTrue(deniedTypes.contains("VILLAGER"));
            assertTrue(deniedTypes.contains("WANDERING_TRADER"));
        }
    }
}
