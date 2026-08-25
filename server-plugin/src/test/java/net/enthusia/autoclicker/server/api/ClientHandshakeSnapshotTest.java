package net.enthusia.autoclicker.server.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ClientHandshakeSnapshotTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-25T12:00:00Z");

    @Test
    void rejectsMissingAndOversizedEvidenceFields() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ClientHandshakeSnapshot("", "fabric", "1.21.11", RECEIVED_AT)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ClientHandshakeSnapshot("1.3.2", "x".repeat(33), "1.21.11", RECEIVED_AT)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ClientHandshakeSnapshot("1.3.2", "fabric", "1.21.11", null)
        );
    }
}
