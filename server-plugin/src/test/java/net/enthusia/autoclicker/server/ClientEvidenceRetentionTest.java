package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.enthusia.autoclicker.server.api.ClientEvidenceValidation;
import org.junit.jupiter.api.Test;

class ClientEvidenceRetentionTest {
    private static final int CONCURRENT_OBSERVATIONS = 500;
    private static final int CONCURRENT_RECORD_BOUND = 128;
    private static final Instant START = Instant.parse("2026-08-27T12:00:00Z");
    private static final Duration RETENTION = Duration.ofMinutes(30L);
    private static final String MOD_VERSION = "1.3.2";
    private static final String LOADER = "fabric";
    private static final String MINECRAFT_VERSION = "1.21.11";

    @Test
    void retainsValidatedEvidenceForBoundedOfflineLookup() {
        MutableClock clock = new MutableClock(START);
        ClientHandshakeService service = new ClientHandshakeService(clock, RETENTION, 10);
        UUID playerId = UUID.randomUUID();
        service.accept(playerId, payload());

        service.markOffline(playerId);

        assertTrue(service.handshake(playerId).isEmpty());
        assertFalse(service.evidence(playerId).currentSession());
        assertEquals(ClientEvidenceValidation.VALID, service.evidence(playerId).validation());

        clock.advance(RETENTION);

        assertEquals(ClientEvidenceValidation.NOT_OBSERVED, service.evidence(playerId).validation());
    }

    @Test
    void evictsTheOldestObservationWhenTheConfiguredBoundIsReached() {
        ClientHandshakeService service = new ClientHandshakeService(
                new MutableClock(START),
                RETENTION,
                2
        );
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        service.accept(first, payload());
        service.accept(second, payload());
        service.accept(third, payload());

        assertEquals(ClientEvidenceValidation.NOT_OBSERVED, service.evidence(first).validation());
        assertEquals(ClientEvidenceValidation.VALID, service.evidence(second).validation());
        assertEquals(ClientEvidenceValidation.VALID, service.evidence(third).validation());
    }

    @Test
    void policyUpdatesImmediatelyApplyTheSmallerRetentionAndRecordBound() {
        MutableClock clock = new MutableClock(START);
        ClientHandshakeService service = new ClientHandshakeService(clock, RETENTION, 3);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        service.accept(first, payload());
        service.accept(second, payload());
        clock.advance(Duration.ofMinutes(10L));

        service.updatePolicy(Duration.ofMinutes(5L), 1);

        assertEquals(ClientEvidenceValidation.NOT_OBSERVED, service.evidence(first).validation());
        assertEquals(ClientEvidenceValidation.NOT_OBSERVED, service.evidence(second).validation());
    }

    @Test
    void rejectsUnboundedOrNonExpiringPolicies() {
        MutableClock clock = new MutableClock(START);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientHandshakeService(clock, Duration.ZERO, 10)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientHandshakeService(clock, RETENTION, 0)
        );
    }

    @Test
    void concurrentObservationsRemainWithinTheConfiguredBound() {
        ClientHandshakeService service = new ClientHandshakeService(
                Clock.fixed(START, ZoneOffset.UTC),
                RETENTION,
                CONCURRENT_RECORD_BOUND
        );
        List<UUID> players = IntStream.range(0, CONCURRENT_OBSERVATIONS)
                .mapToObj(index -> new UUID(0L, index + 1L))
                .toList();

        players.parallelStream().forEach(playerId -> service.accept(playerId, payload()));

        long retained = players.parallelStream()
                .filter(playerId -> service.evidence(playerId).handshakeObserved())
                .count();
        assertEquals(CONCURRENT_RECORD_BOUND, retained);
    }

    private static byte[] payload() {
        return ClientHandshakeTestPayload.create(
                1,
                MOD_VERSION,
                LOADER,
                MINECRAFT_VERSION
        );
    }

    private static final class MutableClock extends Clock {
        private Instant current;
        private final ZoneId zone;

        private MutableClock(Instant current) {
            this(current, ZoneOffset.UTC);
        }

        private MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId targetZone) {
            return new MutableClock(current, targetZone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
