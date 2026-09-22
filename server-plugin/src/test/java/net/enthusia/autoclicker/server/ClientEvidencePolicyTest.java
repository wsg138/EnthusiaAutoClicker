package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

final class ClientEvidencePolicyTest {
    @Test
    void acceptsPositiveRetentionAndRecordLimit() {
        ClientEvidencePolicy policy = new ClientEvidencePolicy(Duration.ofMinutes(30), 250);

        assertEquals(Duration.ofMinutes(30), policy.retention());
        assertEquals(250, policy.maximumRecords());
    }

    @Test
    void rejectsNullZeroAndNegativeRetention() {
        assertThrows(NullPointerException.class, () -> new ClientEvidencePolicy(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientEvidencePolicy(Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientEvidencePolicy(Duration.ofSeconds(-1), 1));
    }

    @Test
    void rejectsNonPositiveRecordLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClientEvidencePolicy(Duration.ofMinutes(1), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientEvidencePolicy(Duration.ofMinutes(1), -1));
    }
}
