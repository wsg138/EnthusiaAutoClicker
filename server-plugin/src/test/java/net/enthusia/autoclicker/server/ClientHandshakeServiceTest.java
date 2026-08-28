package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;
import net.enthusia.autoclicker.server.api.ClientEvidenceValidation;
import net.enthusia.autoclicker.server.api.ClientHandshakeSnapshot;
import org.junit.jupiter.api.Test;

class ClientHandshakeServiceTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-25T12:00:00Z");
    private static final String CURRENT_MOD_VERSION = "1.3.2";
    private static final String CURRENT_MINECRAFT_VERSION = "1.21.11";
    private static final String FABRIC_LOADER = "fabric";

    @Test
    void publishesAValidatedSnapshotForTheCurrentHandshake() {
        ClientHandshakeService service = service();
        UUID playerId = UUID.randomUUID();

        service.accept(playerId, payload(1, CURRENT_MOD_VERSION, FABRIC_LOADER, CURRENT_MINECRAFT_VERSION));

        assertEquals(
            new ClientHandshakeSnapshot(CURRENT_MOD_VERSION, FABRIC_LOADER, CURRENT_MINECRAFT_VERSION, RECEIVED_AT),
            service.handshake(playerId).orElseThrow()
        );
        assertEquals(1, service.apiVersion());
        assertEquals(ClientEvidenceValidation.VALID, service.evidence(playerId).validation());
    }

    @Test
    void invalidHandshakeRemovesStaleEvidence() {
        ClientHandshakeService service = service();
        UUID playerId = UUID.randomUUID();
        service.accept(playerId, payload(1, CURRENT_MOD_VERSION, FABRIC_LOADER, CURRENT_MINECRAFT_VERSION));

        service.accept(playerId, payload(2, CURRENT_MOD_VERSION, FABRIC_LOADER, CURRENT_MINECRAFT_VERSION));

        assertTrue(service.handshake(playerId).isEmpty());
        assertEquals(
                ClientEvidenceValidation.UNSUPPORTED_PROTOCOL,
                service.evidence(playerId).validation()
        );
    }

    @Test
    void malformedHandshakeIsRejectedWithoutLeakingAnException() {
        ClientHandshakeService service = service();
        UUID playerId = UUID.randomUUID();

        service.accept(playerId, new byte[] {1, (byte) 0x80});

        assertTrue(service.handshake(playerId).isEmpty());
        assertEquals(ClientEvidenceValidation.MALFORMED, service.evidence(playerId).validation());
    }

    @Test
    void trailingOrInvalidUtfDataCannotBecomeValidatedEvidence() {
        ClientHandshakeService service = service();
        UUID playerId = UUID.randomUUID();
        byte[] valid = payload(1, CURRENT_MOD_VERSION, FABRIC_LOADER, CURRENT_MINECRAFT_VERSION);
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);

        service.accept(playerId, trailing);
        assertEquals(ClientEvidenceValidation.MALFORMED, service.evidence(playerId).validation());

        byte[] invalidUtf = valid.clone();
        invalidUtf[2] = (byte) 0xC3;
        service.accept(playerId, invalidUtf);
        assertEquals(ClientEvidenceValidation.MALFORMED, service.evidence(playerId).validation());
    }

    @Test
    void clearRemovesEveryPublishedSnapshot() {
        ClientHandshakeService service = service();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        service.accept(first, payload(1, CURRENT_MOD_VERSION, FABRIC_LOADER, CURRENT_MINECRAFT_VERSION));
        service.accept(second, payload(1, CURRENT_MOD_VERSION, "neoforge", "26.1"));

        service.clear();

        assertTrue(service.handshake(first).isEmpty());
        assertTrue(service.handshake(second).isEmpty());
        assertEquals(ClientEvidenceValidation.NOT_OBSERVED, service.evidence(first).validation());
    }

    @Test
    void nullPlayerIdIsRejectedAtTheApiBoundary() {
        ClientHandshakeService service = service();

        assertThrows(NullPointerException.class, () -> service.handshake((UUID) null));
        assertThrows(NullPointerException.class, () -> service.evidence(null));
    }

    private static ClientHandshakeService service() {
        return new ClientHandshakeService(Clock.fixed(RECEIVED_AT, ZoneOffset.UTC));
    }

    private static byte[] payload(
            int protocol,
            String modVersion,
            String loader,
            String minecraftVersion
    ) {
        return ClientHandshakeTestPayload.create(
                protocol,
                modVersion,
                loader,
                minecraftVersion
        );
    }
}
