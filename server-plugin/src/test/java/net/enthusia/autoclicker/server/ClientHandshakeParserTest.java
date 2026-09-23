package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.enthusia.autoclicker.server.api.ClientEvidenceSnapshot;
import net.enthusia.autoclicker.server.api.ClientEvidenceValidation;
import net.enthusia.autoclicker.server.api.EnthusiaAutoClickerClientApi;
import org.junit.jupiter.api.Test;

final class ClientHandshakeParserTest {
    private final ClientHandshakeParser parser = new ClientHandshakeParser();
    private final UUID playerId = UUID.fromString("12345678-1234-5678-1234-567812345678");
    private final Instant observedAt = Instant.parse("2026-09-22T12:00:00Z");

    @Test
    void parsesValidHandshakeIntoBoundedEvidence() {
        byte[] payload = ClientHandshakeTestPayload.create(
                EnthusiaAutoClickerClientApi.HANDSHAKE_PROTOCOL_VERSION,
                "1.2.3",
                "fabric",
                "1.21.11"
        );

        ClientEvidenceSnapshot evidence = parser.parse(playerId, payload, observedAt);

        assertEquals(ClientEvidenceValidation.VALID, evidence.validation());
        assertEquals(playerId, evidence.playerId());
        assertEquals(EnthusiaAutoClickerClientApi.EVIDENCE_VERSION, evidence.evidenceVersion());
        assertEquals(EnthusiaAutoClickerClientApi.HANDSHAKE_PROTOCOL_VERSION,
                evidence.handshakeProtocolVersion());
        assertEquals("1.2.3", evidence.modVersion().orElseThrow());
        assertEquals("fabric", evidence.loader().orElseThrow());
        assertEquals("1.21.11", evidence.minecraftVersion().orElseThrow());
        assertEquals(observedAt, evidence.observedAt().orElseThrow());
        assertTrue(evidence.currentSession());
        assertTrue(evidence.validatedHandshake().isPresent());
    }

    @Test
    void unsupportedProtocolPreservesObservedProtocolButNoClientFields() {
        byte[] payload = ClientHandshakeTestPayload.create(99, "1.2.3", "fabric", "1.21.11");

        ClientEvidenceSnapshot evidence = parser.parse(playerId, payload, observedAt);

        assertEquals(ClientEvidenceValidation.UNSUPPORTED_PROTOCOL, evidence.validation());
        assertEquals(99, evidence.handshakeProtocolVersion());
        assertTrue(evidence.modVersion().isEmpty());
        assertTrue(evidence.loader().isEmpty());
        assertTrue(evidence.minecraftVersion().isEmpty());
        assertTrue(evidence.validatedHandshake().isEmpty());
    }

    @Test
    void emptyPayloadIsMalformedWithUnknownProtocol() {
        ClientEvidenceSnapshot evidence = parser.parse(playerId, new byte[0], observedAt);

        assertEquals(ClientEvidenceValidation.MALFORMED, evidence.validation());
        assertEquals(ClientEvidenceSnapshot.UNKNOWN_PROTOCOL_VERSION,
                evidence.handshakeProtocolVersion());
    }

    @Test
    void payloadLargerThanHardLimitIsMalformedWithoutParsingFields() {
        byte[] payload = new byte[1025];
        payload[0] = (byte) EnthusiaAutoClickerClientApi.HANDSHAKE_PROTOCOL_VERSION;

        ClientEvidenceSnapshot evidence = parser.parse(playerId, payload, observedAt);

        assertEquals(ClientEvidenceValidation.MALFORMED, evidence.validation());
        assertEquals(EnthusiaAutoClickerClientApi.HANDSHAKE_PROTOCOL_VERSION,
                evidence.handshakeProtocolVersion());
    }

    @Test
    void trailingBytesMakeOtherwiseValidHandshakeMalformed() {
        byte[] valid = ClientHandshakeTestPayload.create(1, "1.0", "fabric", "1.21.11");
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        trailing[trailing.length - 1] = 42;

        ClientEvidenceSnapshot evidence = parser.parse(playerId, trailing, observedAt);

        assertEquals(ClientEvidenceValidation.MALFORMED, evidence.validation());
    }

    @Test
    void blankFieldMakesHandshakeMalformed() {
        ClientEvidenceSnapshot evidence = parser.parse(
                playerId,
                ClientHandshakeTestPayload.create(1, " ", "fabric", "1.21.11"),
                observedAt
        );

        assertEquals(ClientEvidenceValidation.MALFORMED, evidence.validation());
        assertTrue(evidence.modVersion().isEmpty());
    }

    @Test
    void truncatedHandshakeIsMalformed() {
        byte[] valid = ClientHandshakeTestPayload.create(1, "1.0", "fabric", "1.21.11");
        byte[] truncated = Arrays.copyOf(valid, valid.length - 2);

        assertEquals(ClientEvidenceValidation.MALFORMED,
                parser.parse(playerId, truncated, observedAt).validation());
    }

    @Test
    void parserRejectsNullInputsRatherThanProducingAmbiguousEvidence() {
        byte[] valid = ClientHandshakeTestPayload.create(1, "1.0", "fabric", "1.21.11");

        assertThrows(NullPointerException.class, () -> parser.parse(null, valid, observedAt));
        assertThrows(NullPointerException.class, () -> parser.parse(playerId, null, observedAt));
        assertThrows(NullPointerException.class, () -> parser.parse(playerId, valid, null));
    }

    @Test
    void malformedEvidenceStillRecordsThatHandshakeWasObservedInCurrentSession() {
        ClientEvidenceSnapshot evidence = parser.parse(playerId, new byte[] {1}, observedAt);

        assertEquals(ClientEvidenceValidation.MALFORMED, evidence.validation());
        assertTrue(evidence.handshakeObserved());
        assertTrue(evidence.currentSession());
        assertFalse(evidence.observedAt().isEmpty());
    }
}
