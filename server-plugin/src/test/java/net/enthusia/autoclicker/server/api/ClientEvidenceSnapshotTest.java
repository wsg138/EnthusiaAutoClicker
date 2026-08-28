package net.enthusia.autoclicker.server.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientEvidenceSnapshotTest {
    private static final int EVIDENCE_VERSION = 1;
    private static final int HANDSHAKE_PROTOCOL_VERSION = 1;
    private static final Instant OBSERVED_AT = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void exposesValidatedHandshakeFieldsWithoutLosingSchemaState() {
        UUID playerId = UUID.randomUUID();

        ClientEvidenceSnapshot snapshot = valid(playerId);

        assertEquals(playerId, snapshot.playerId());
        assertEquals(EVIDENCE_VERSION, snapshot.evidenceVersion());
        assertEquals(HANDSHAKE_PROTOCOL_VERSION, snapshot.handshakeProtocolVersion());
        assertEquals(ClientEvidenceValidation.VALID, snapshot.validation());
        assertTrue(snapshot.handshakeObserved());
        assertEquals(
                new ClientHandshakeSnapshot("1.3.2", "fabric", "1.21.11", OBSERVED_AT),
                snapshot.validatedHandshake().orElseThrow()
        );
    }

    @Test
    void representsAnUnknownPlayerWithoutInventingEvidence() {
        ClientEvidenceSnapshot snapshot = ClientEvidenceSnapshot.notObserved(
                UUID.randomUUID(),
                EVIDENCE_VERSION
        );

        assertEquals(ClientEvidenceValidation.NOT_OBSERVED, snapshot.validation());
        assertFalse(snapshot.handshakeObserved());
        assertFalse(snapshot.currentSession());
        assertTrue(snapshot.observedAt().isEmpty());
        assertTrue(snapshot.validatedHandshake().isEmpty());
    }

    @Test
    void marksRetainedEvidenceAsBelongingToAPreviousSession() {
        ClientEvidenceSnapshot previousSession = valid(UUID.randomUUID()).asPreviousSession();

        assertFalse(previousSession.currentSession());
        assertEquals(ClientEvidenceValidation.VALID, previousSession.validation());
        assertEquals(OBSERVED_AT, previousSession.observedAt().orElseThrow());
    }

    @Test
    void rejectsContradictoryEvidenceShapes() {
        UUID playerId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientEvidenceSnapshot(
                        playerId,
                        EVIDENCE_VERSION,
                        HANDSHAKE_PROTOCOL_VERSION,
                        ClientEvidenceValidation.VALID,
                        Optional.empty(),
                        Optional.of("fabric"),
                        Optional.of("1.21.11"),
                        Optional.of(OBSERVED_AT),
                        true
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ClientEvidenceSnapshot.invalid(
                        playerId,
                        EVIDENCE_VERSION,
                        HANDSHAKE_PROTOCOL_VERSION,
                        ClientEvidenceValidation.VALID,
                        OBSERVED_AT,
                        true
                )
        );
    }

    @Test
    void defaultEvidenceQueryKeepsExistingApiImplementationsCompatible() {
        UUID playerId = UUID.randomUUID();
        EnthusiaAutoClickerClientApi legacyImplementation = requestedPlayer -> Optional.of(
                new ClientHandshakeSnapshot("1.3.2", "fabric", "1.21.11", OBSERVED_AT)
        );

        ClientEvidenceSnapshot evidence = legacyImplementation.evidence(playerId);

        assertEquals(ClientEvidenceValidation.VALID, evidence.validation());
        assertTrue(evidence.currentSession());
        assertEquals(HANDSHAKE_PROTOCOL_VERSION, evidence.handshakeProtocolVersion());
    }

    private static ClientEvidenceSnapshot valid(UUID playerId) {
        return ClientEvidenceSnapshot.valid(
                playerId,
                EVIDENCE_VERSION,
                HANDSHAKE_PROTOCOL_VERSION,
                "1.3.2",
                "fabric",
                "1.21.11",
                OBSERVED_AT,
                true
        );
    }
}
