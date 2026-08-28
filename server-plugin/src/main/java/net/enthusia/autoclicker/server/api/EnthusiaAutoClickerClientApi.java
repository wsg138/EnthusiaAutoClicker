package net.enthusia.autoclicker.server.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface EnthusiaAutoClickerClientApi {
    int API_VERSION = 1;
    int EVIDENCE_VERSION = 1;

    default int apiVersion() {
        return API_VERSION;
    }

    Optional<ClientHandshakeSnapshot> handshake(UUID playerId);

    default ClientEvidenceSnapshot evidence(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return handshake(playerId)
                .map(snapshot -> ClientEvidenceSnapshot.valid(
                        playerId,
                        EVIDENCE_VERSION,
                        1,
                        snapshot.modVersion(),
                        snapshot.loader(),
                        snapshot.minecraftVersion(),
                        snapshot.receivedAt(),
                        true
                ))
                .orElseGet(() -> ClientEvidenceSnapshot.notObserved(playerId, EVIDENCE_VERSION));
    }
}
