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
    private static final int MAX_PROTOCOL_VERSION = 255;
    private static final int MAX_MOD_VERSION_LENGTH = 64;
    private static final int MAX_LOADER_LENGTH = 32;
    private static final int MAX_MINECRAFT_VERSION_LENGTH = 32;

    public ClientEvidenceSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(validation, "validation");
        modVersion = validated(modVersion, "modVersion", MAX_MOD_VERSION_LENGTH);
        loader = validated(loader, "loader", MAX_LOADER_LENGTH);
        minecraftVersion = validated(
                minecraftVersion,
                "minecraftVersion",
                MAX_MINECRAFT_VERSION_LENGTH
        );
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        if (evidenceVersion < 1) {
            throw new IllegalArgumentException("evidenceVersion must be positive");
        }
        if (handshakeProtocolVersion < UNKNOWN_PROTOCOL_VERSION
                || handshakeProtocolVersion > MAX_PROTOCOL_VERSION) {
            throw new IllegalArgumentException("handshakeProtocolVersion must fit one unsigned byte");
        }
        validateShape(
                validation,
                handshakeProtocolVersion,
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

    private static Optional<String> validated(
            Optional<String> value,
            String fieldName,
            int maximumLength
    ) {
        Optional<String> required = Objects.requireNonNull(value, fieldName);
        required.ifPresent(field -> {
            if (field.isBlank() || field.length() > maximumLength) {
                throw new IllegalArgumentException(fieldName + " is invalid");
            }
        });
        return required;
    }

    private static void validateShape(
            ClientEvidenceValidation validation,
            int handshakeProtocolVersion,
            Optional<String> modVersion,
            Optional<String> loader,
            Optional<String> minecraftVersion,
            Optional<Instant> observedAt,
            boolean currentSession
    ) {
        boolean hasAllClientFields = modVersion.isPresent()
                && loader.isPresent()
                && minecraftVersion.isPresent();
        boolean hasAnyClientField = modVersion.isPresent()
                || loader.isPresent()
                || minecraftVersion.isPresent();
        if (validation == ClientEvidenceValidation.VALID) {
            if (!hasAllClientFields || observedAt.isEmpty()) {
                throw new IllegalArgumentException("valid evidence requires every handshake field");
            }
            return;
        }
        if (hasAnyClientField) {
            throw new IllegalArgumentException("invalid evidence cannot contain client version fields");
        }
        if (validation == ClientEvidenceValidation.NOT_OBSERVED) {
            if (handshakeProtocolVersion != UNKNOWN_PROTOCOL_VERSION
                    || observedAt.isPresent()
                    || currentSession) {
                throw new IllegalArgumentException("unobserved evidence cannot contain observation state");
            }
        } else if (observedAt.isEmpty()) {
            throw new IllegalArgumentException("observed evidence requires a timestamp");
        }
    }
}
