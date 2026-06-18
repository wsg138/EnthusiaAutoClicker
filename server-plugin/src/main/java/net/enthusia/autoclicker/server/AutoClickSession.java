package net.enthusia.autoclicker.server;

import java.util.UUID;
import org.bukkit.Location;

final class AutoClickSession {
    private final UUID playerId;
    private final Location anchor;
    private final AutoClickMode mode;
    private final int intervalTicks;
    private int ticksUntilNextAttack;

    AutoClickSession(UUID playerId, Location anchor, AutoClickMode mode, int intervalTicks) {
        this.playerId = playerId;
        this.anchor = anchor.clone();
        this.mode = mode;
        this.intervalTicks = intervalTicks;
    }

    UUID playerId() {
        return playerId;
    }

    Location anchor() {
        return anchor;
    }

    AutoClickMode mode() {
        return mode;
    }

    int intervalTicks() {
        return intervalTicks;
    }

    boolean consumeFixedIntervalTick() {
        if (mode != AutoClickMode.FIXED_INTERVAL) {
            return false;
        }
        if (ticksUntilNextAttack > 0) {
            ticksUntilNextAttack--;
            return false;
        }
        ticksUntilNextAttack = Math.max(0, intervalTicks - 1);
        return true;
    }
}
