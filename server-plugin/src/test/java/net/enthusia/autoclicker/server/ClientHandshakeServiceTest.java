package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import net.enthusia.autoclicker.server.api.ClientHandshakeSnapshot;
import org.junit.jupiter.api.Test;

class ClientHandshakeServiceTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-25T12:00:00Z");
    private static final String CURRENT_MINECRAFT_VERSION = "1.21.11";

    @Test
    void publishesAValidatedSnapshotForTheCurrentHandshake() {
        ClientHandshakeService service = service();
        UUID playerId = UUID.randomUUID();

        service.accept(playerId, handshake(1, "1.3.2", "fabric", CURRENT_MINECRAFT_VERSION));

        assertEquals(
            new ClientHandshakeSnapshot("1.3.2", "fabric", CURRENT_MINECRAFT_VERSION, RECEIVED_AT),
            service.handshake(playerId).orElseThrow()
        );
        assertEquals(1, service.apiVersion());
    }

    @Test
    void invalidHandshakeRemovesStaleEvidence() {
        ClientHandshakeService service = service();
        UUID playerId = UUID.randomUUID();
        service.accept(playerId, handshake(1, "1.3.2", "fabric", CURRENT_MINECRAFT_VERSION));

        service.accept(playerId, handshake(2, "1.3.2", "fabric", CURRENT_MINECRAFT_VERSION));

        assertTrue(service.handshake(playerId).isEmpty());
    }

    @Test
    void malformedHandshakeIsRejectedWithoutLeakingAnException() {
        ClientHandshakeService service = service();
        UUID playerId = UUID.randomUUID();

        service.accept(playerId, new byte[] {1, (byte) 0x80});

        assertTrue(service.handshake(playerId).isEmpty());
    }

    @Test
    void clearRemovesEveryPublishedSnapshot() {
        ClientHandshakeService service = service();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        service.accept(first, handshake(1, "1.3.2", "fabric", CURRENT_MINECRAFT_VERSION));
        service.accept(second, handshake(1, "1.3.2", "neoforge", "26.1"));

        service.clear();

        assertTrue(service.handshake(first).isEmpty());
        assertTrue(service.handshake(second).isEmpty());
    }

    @Test
    void nullPlayerIdIsRejectedAtTheApiBoundary() {
        ClientHandshakeService service = service();

        assertThrows(NullPointerException.class, () -> service.handshake((UUID) null));
    }

    private static ClientHandshakeService service() {
        return new ClientHandshakeService(Clock.fixed(RECEIVED_AT, ZoneOffset.UTC));
    }

    private static byte[] handshake(int protocol, String modVersion, String loader, String minecraftVersion) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(protocol);
        writeUtf(output, modVersion);
        writeUtf(output, loader);
        writeUtf(output, minecraftVersion);
        return output.toByteArray();
    }

    private static void writeUtf(ByteArrayOutputStream output, String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(output, encoded.length);
        output.writeBytes(encoded);
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
        int remaining = value;
        do {
            int next = remaining & 0x7F;
            remaining >>>= 7;
            if (remaining != 0) {
                next |= 0x80;
            }
            output.write(next);
        } while (remaining != 0);
    }
}
