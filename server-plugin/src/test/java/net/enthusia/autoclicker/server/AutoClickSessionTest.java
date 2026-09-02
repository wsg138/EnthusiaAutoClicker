package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class AutoClickSessionTest {
    @Test
    void fixedIntervalSessionAttacksImmediatelyAndThenWaits() {
        AutoClickSession session = new AutoClickSession(
                UUID.randomUUID(), new Location(null, 0, 0, 0), AutoClickMode.FIXED_INTERVAL, 3);

        assertTrue(session.consumeFixedIntervalTick());
        assertFalse(session.consumeFixedIntervalTick());
        assertFalse(session.consumeFixedIntervalTick());
        assertTrue(session.consumeFixedIntervalTick());
    }

    @Test
    void cooldownSessionDoesNotConsumeFixedIntervalTicks() {
        AutoClickSession session = new AutoClickSession(
                UUID.randomUUID(), new Location(null, 0, 0, 0), AutoClickMode.COOLDOWN, 0);

        assertFalse(session.consumeFixedIntervalTick());
        assertFalse(session.consumeFixedIntervalTick());
    }
}
