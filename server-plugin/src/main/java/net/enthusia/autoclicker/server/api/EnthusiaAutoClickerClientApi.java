package net.enthusia.autoclicker.server.api;

import java.util.Optional;
import java.util.UUID;

public interface EnthusiaAutoClickerClientApi {
    int API_VERSION = 1;

    default int apiVersion() {
        return API_VERSION;
    }

    Optional<ClientHandshakeSnapshot> handshake(UUID playerId);
}
