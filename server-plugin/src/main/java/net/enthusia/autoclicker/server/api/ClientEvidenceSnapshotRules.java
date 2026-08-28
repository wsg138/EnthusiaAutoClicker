package net.enthusia.autoclicker.server.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

final class ClientEvidenceSnapshotRules {
    private static final int MINIMUM_EVIDENCE_VERSION = 1;
    private static final int MAXIMUM_PROTOCOL_VERSION = 255;
    private static final int MAXIMUM_MOD_VERSION_LENGTH = 64;
    private static final int MAXIMUM_LOADER_LENGTH = 32;
    private static final int MAXIMUM_MINECRAFT_VERSION_LENGTH = 32;

    private ClientEvidenceSnapshotRules() {
    }

    static Optional<String> versionField(
            Optional<String> value,
            String fieldName,
            VersionField field
    ) {
        Optional<String> required = Objects.requireNonNull(value, fieldName);
        required.ifPresent(version -> {
            if (version.isBlank() || version.length() > field.maximumLength) {
                throw new IllegalArgumentException(fieldName + " is invalid");
            }
        });
        return required;
    }

    static void validate(
            int evidenceVersion,
            int handshakeProtocolVersion,
            ClientEvidenceValidation validation,
            Optional<String> modVersion,
            Optional<String> loader,
            Optional<String> minecraftVersion,
            Optional<Instant> observedAt,
            boolean currentSession
    ) {
        validateVersionNumbers(evidenceVersion, handshakeProtocolVersion);
        if (validation == ClientEvidenceValidation.VALID) {
            validateValidEvidence(
                    handshakeProtocolVersion,
                    modVersion,
                    loader,
                    minecraftVersion,
                    observedAt
            );
            return;
        }
        validateInvalidEvidence(
                handshakeProtocolVersion,
                validation,
                modVersion,
                loader,
                minecraftVersion,
                observedAt,
                currentSession
        );
    }

    private static void validateVersionNumbers(
            int evidenceVersion,
            int handshakeProtocolVersion
    ) {
        if (evidenceVersion < MINIMUM_EVIDENCE_VERSION) {
            throw new IllegalArgumentException("evidenceVersion must be positive");
        }
        if (handshakeProtocolVersion < ClientEvidenceSnapshot.UNKNOWN_PROTOCOL_VERSION
                || handshakeProtocolVersion > MAXIMUM_PROTOCOL_VERSION) {
            throw new IllegalArgumentException("handshakeProtocolVersion must fit one unsigned byte");
        }
    }

    private static void validateValidEvidence(
            int handshakeProtocolVersion,
            Optional<String> modVersion,
            Optional<String> loader,
            Optional<String> minecraftVersion,
            Optional<Instant> observedAt
    ) {
        boolean hasAllClientFields = modVersion.isPresent()
                && loader.isPresent()
                && minecraftVersion.isPresent();
        if (handshakeProtocolVersion == ClientEvidenceSnapshot.UNKNOWN_PROTOCOL_VERSION
                || !hasAllClientFields
                || observedAt.isEmpty()) {
            throw new IllegalArgumentException("valid evidence requires every handshake field");
        }
    }

    private static void validateInvalidEvidence(
            int handshakeProtocolVersion,
            ClientEvidenceValidation validation,
            Optional<String> modVersion,
            Optional<String> loader,
            Optional<String> minecraftVersion,
            Optional<Instant> observedAt,
            boolean currentSession
    ) {
        boolean hasAnyClientField = modVersion.isPresent()
                || loader.isPresent()
                || minecraftVersion.isPresent();
        if (hasAnyClientField) {
            throw new IllegalArgumentException("invalid evidence cannot contain client version fields");
        }
        validateObservationState(
                handshakeProtocolVersion,
                validation,
                observedAt,
                currentSession
        );
    }

    private static void validateObservationState(
            int handshakeProtocolVersion,
            ClientEvidenceValidation validation,
            Optional<Instant> observedAt,
            boolean currentSession
    ) {
        if (validation == ClientEvidenceValidation.NOT_OBSERVED) {
            if (handshakeProtocolVersion != ClientEvidenceSnapshot.UNKNOWN_PROTOCOL_VERSION
                    || observedAt.isPresent()
                    || currentSession) {
                throw new IllegalArgumentException("unobserved evidence cannot contain observation state");
            }
        } else if (observedAt.isEmpty()) {
            throw new IllegalArgumentException("observed evidence requires a timestamp");
        }
    }

    enum VersionField {
        MOD(MAXIMUM_MOD_VERSION_LENGTH),
        LOADER(MAXIMUM_LOADER_LENGTH),
        MINECRAFT(MAXIMUM_MINECRAFT_VERSION_LENGTH);

        private final int maximumLength;

        VersionField(int maximumLength) {
            this.maximumLength = maximumLength;
        }
    }
}
