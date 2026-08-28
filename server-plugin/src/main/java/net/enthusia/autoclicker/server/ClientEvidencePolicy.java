package net.enthusia.autoclicker.server;

import java.time.Duration;
import java.util.Objects;

record ClientEvidencePolicy(Duration retention, int maximumRecords) {
    ClientEvidencePolicy {
        Objects.requireNonNull(retention, "retention");
        if (retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("retention must be positive");
        }
        if (maximumRecords < 1) {
            throw new IllegalArgumentException("maximumRecords must be positive");
        }
    }
}
