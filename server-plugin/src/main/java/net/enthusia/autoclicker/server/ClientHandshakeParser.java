package net.enthusia.autoclicker.server;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import net.enthusia.autoclicker.server.api.ClientEvidenceSnapshot;
import net.enthusia.autoclicker.server.api.ClientEvidenceValidation;
import net.enthusia.autoclicker.server.api.EnthusiaAutoClickerClientApi;

final class ClientHandshakeParser {
    private static final int MAXIMUM_PAYLOAD_BYTES = 1024;
    private static final int MAXIMUM_MOD_VERSION_LENGTH = 64;
    private static final int MAXIMUM_LOADER_LENGTH = 32;
    private static final int MAXIMUM_MINECRAFT_VERSION_LENGTH = 32;

    ClientEvidenceSnapshot parse(UUID playerId, byte[] message, Instant observedAt) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(observedAt, "observedAt");
        int protocol = protocolVersion(message);
        if (message.length == 0 || message.length > MAXIMUM_PAYLOAD_BYTES) {
            return invalid(playerId, protocol, ClientEvidenceValidation.MALFORMED, observedAt);
        }
        try {
            HandshakePayloadCursor cursor = new HandshakePayloadCursor(message);
            cursor.readUnsignedByte();
            if (protocol != EnthusiaAutoClickerClientApi.HANDSHAKE_PROTOCOL_VERSION) {
                return invalid(
                        playerId,
                        protocol,
                        ClientEvidenceValidation.UNSUPPORTED_PROTOCOL,
                        observedAt
                );
            }
            ClientEvidenceSnapshot evidence = ClientEvidenceSnapshot.valid(
                    playerId,
                    EnthusiaAutoClickerClientApi.EVIDENCE_VERSION,
                    protocol,
                    cursor.readUtf(MAXIMUM_MOD_VERSION_LENGTH),
                    cursor.readUtf(MAXIMUM_LOADER_LENGTH),
                    cursor.readUtf(MAXIMUM_MINECRAFT_VERSION_LENGTH),
                    observedAt,
                    true
            );
            if (cursor.hasRemaining()) {
                return invalid(playerId, protocol, ClientEvidenceValidation.MALFORMED, observedAt);
            }
            return evidence;
        } catch (RuntimeException exception) {
            return invalid(playerId, protocol, ClientEvidenceValidation.MALFORMED, observedAt);
        }
    }

    private static ClientEvidenceSnapshot invalid(
            UUID playerId,
            int protocol,
            ClientEvidenceValidation validation,
            Instant observedAt
    ) {
        return ClientEvidenceSnapshot.invalid(
                playerId,
                EnthusiaAutoClickerClientApi.EVIDENCE_VERSION,
                protocol,
                validation,
                observedAt,
                true
        );
    }

    private static int protocolVersion(byte[] message) {
        return message.length == 0
                ? ClientEvidenceSnapshot.UNKNOWN_PROTOCOL_VERSION
                : Byte.toUnsignedInt(message[0]);
    }
}
