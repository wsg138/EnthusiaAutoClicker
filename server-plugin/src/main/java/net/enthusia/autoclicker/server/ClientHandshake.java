package net.enthusia.autoclicker.server;

import java.time.Instant;

record ClientHandshake(
    String modVersion,
    String loader,
    String minecraftVersion,
    Instant receivedAt
) {
}
