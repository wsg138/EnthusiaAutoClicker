package net.enthusia.autoclicker.server.api;

import java.time.Instant;

public record ClientHandshakeSnapshot(
        String modVersion,
        String loader,
        String minecraftVersion,
        Instant receivedAt
) {
    public ClientHandshakeSnapshot {
        if (modVersion == null || modVersion.isBlank() || modVersion.length() > 64
                || loader == null || loader.isBlank() || loader.length() > 32
                || minecraftVersion == null || minecraftVersion.isBlank()
                || minecraftVersion.length() > 32 || receivedAt == null) {
            throw new IllegalArgumentException("handshake snapshot fields are invalid");
        }
    }
}
