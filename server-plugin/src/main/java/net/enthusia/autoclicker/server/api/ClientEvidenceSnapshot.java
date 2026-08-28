package net.enthusia.autoclicker.server.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded client-handshake evidence published for moderation integrations.
 *
 * <p>A snapshot reports only the small handshake fields sent by the official client mod. It is a
 * convenience signal, not proof that the client is trusted or free of other modifications.</p>
 */
public record ClientEvidenceSnapshot(
        UUID playerId,
        int evidenceVersion,
        int handshakeProtocolVersion,
        ClientEvidenceValidation validation,
        Optional<String> modVersion,
        Optional<String> loader,
        Optional<String> minecraftVersion,
        Optional<Instant> observedAt,
        boolean currentSession
) {
    public static final int UNKNOWN_PROTOCOL_VERSION = 0;

    public ClientEvidenceSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(validation, "validation");
        modVersion = ClientEvidenceSnapshotRules.versionField(
                modVersion,
                "modVersion",
                ClientEvidenceSnapshotRules.VersionField.MOD
        );
        loader = ClientEvidenceSnapshotRules.versionField(
                loader,
                "loader",
                ClientEvidenceSnapshotRules.VersionField.LOADER
        );
        minecraftVersion = ClientEvidenceSnapshotRules.versionField(
                minecraftVersion,
                "minecraftVersion",
                ClientEvidenceSnapshotRules.VersionField.MINECRAFT
        );
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        ClientEvidenceSnapshotRules.validateSnapshot(
                evidenceVersion,
                handshakeProtocolVersion,
                validation,
                modVersion,
                loader,
                minecraftVersion,
                observedAt,
                currentSession
        );
    }

    public static ClientEvidenceSnapshot notObserved(UUID playerId, int evidenceVersion) {
        return new ClientEvidenceSnapshot(
                playerId,
                evidenceVersion,
                UNKNOWN_PROTOCOL_VERSION,
                ClientEvidenceValidation.NOT_OBSERVED,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false
        );
    }

    public static ClientEvidenceSnapshot valid(
            UUID playerId,
            int evidenceVersion,
            int handshakeProtocolVersion,
            String modVersion,
            String loader,
            String minecraftVersion,
            Instant observedAt,
            boolean currentSession
    ) {
        return new ClientEvidenceSnapshot(
                playerId,
                evidenceVersion,
                handshakeProtocolVersion,
                ClientEvidenceValidation.VALID,
                Optional.of(modVersion),
                Optional.of(loader),
                Optional.of(minecraftVersion),
                Optional.of(observedAt),
                currentSession
        );
    }

    public static ClientEvidenceSnapshot invalid(
            UUID playerId,
            int evidenceVersion,
            int handshakeProtocolVersion,
            ClientEvidenceValidation validation,
            Instant observedAt,
            boolean currentSession
    ) {
        if (validation != ClientEvidenceValidation.UNSUPPORTED_PROTOCOL
                && validation != ClientEvidenceValidation.MALFORMED) {
            throw new IllegalArgumentException("invalid evidence must have an invalid validation state");
        }
        return new ClientEvidenceSnapshot(
                playerId,
                evidenceVersion,
                handshakeProtocolVersion,
                validation,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(observedAt),
                currentSession
        );
    }

    public boolean handshakeObserved() {
        return validation != ClientEvidenceValidation.NOT_OBSERVED;
    }

    public Optional<ClientHandshakeSnapshot> validatedHandshake() {
        if (validation != ClientEvidenceValidation.VALID) {
            return Optional.empty();
        }
        return Optional.of(new ClientHandshakeSnapshot(
                modVersion.orElseThrow(),
                loader.orElseThrow(),
                minecraftVersion.orElseThrow(),
                observedAt.orElseThrow()
        ));
    }

    public ClientEvidenceSnapshot asPreviousSession() {
        if (!currentSession) {
            return this;
        }
        return new ClientEvidenceSnapshot(
                playerId,
                evidenceVersion,
                handshakeProtocolVersion,
                validation,
                modVersion,
                loader,
                minecraftVersion,
                observedAt,
                false
        );
    }

}
