package net.enthusia.autoclicker.server;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.enthusia.autoclicker.server.api.ClientEvidenceSnapshot;
import net.enthusia.autoclicker.server.api.ClientHandshakeSnapshot;
import net.enthusia.autoclicker.server.api.EnthusiaAutoClickerClientApi;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

final class ClientHandshakeService implements PluginMessageListener, EnthusiaAutoClickerClientApi {
    static final String CHANNEL = "enthusia_autoclicker:handshake";
    private static final Duration DEFAULT_EVIDENCE_RETENTION = Duration.ofMinutes(30L);
    private static final int DEFAULT_MAXIMUM_EVIDENCE_RECORDS = 2048;

    private final Clock clock;
    private final ClientHandshakeParser parser;
    private final ClientEvidenceStore evidenceStore;

    ClientHandshakeService() {
        this(
                Clock.systemUTC(),
                DEFAULT_EVIDENCE_RETENTION,
                DEFAULT_MAXIMUM_EVIDENCE_RECORDS
        );
    }

    ClientHandshakeService(Clock clock) {
        this(clock, DEFAULT_EVIDENCE_RETENTION, DEFAULT_MAXIMUM_EVIDENCE_RECORDS);
    }

    ClientHandshakeService(Clock clock, Duration evidenceRetention, int maximumEvidenceRecords) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.parser = new ClientHandshakeParser();
        this.evidenceStore = new ClientEvidenceStore(
                new ClientEvidencePolicy(evidenceRetention, maximumEvidenceRecords)
        );
    }

    @Override
    public void onPluginMessageReceived(
            @NotNull String channel,
            @NotNull Player player,
            byte @NotNull [] message
    ) {
        if (CHANNEL.equals(channel)) {
            accept(player.getUniqueId(), message);
        }
    }

    void accept(UUID playerId, byte[] message) {
        Instant observedAt = clock.instant();
        evidenceStore.store(parser.parse(playerId, message, observedAt), observedAt);
    }

    Optional<ClientHandshake> handshake(Player player) {
        Objects.requireNonNull(player, "player");
        return handshake(player.getUniqueId()).map(value -> new ClientHandshake(
                value.modVersion(),
                value.loader(),
                value.minecraftVersion(),
                value.receivedAt()
        ));
    }

    @Override
    public Optional<ClientHandshakeSnapshot> handshake(UUID playerId) {
        ClientEvidenceSnapshot evidence = evidence(playerId);
        return evidence.currentSession() ? evidence.validatedHandshake() : Optional.empty();
    }

    @Override
    public ClientEvidenceSnapshot evidence(UUID playerId) {
        return evidenceStore.find(playerId, clock.instant());
    }

    void markOffline(UUID playerId) {
        evidenceStore.markOffline(playerId, clock.instant());
    }

    void updatePolicy(Duration evidenceRetention, int maximumEvidenceRecords) {
        evidenceStore.updatePolicy(
                new ClientEvidencePolicy(evidenceRetention, maximumEvidenceRecords),
                clock.instant()
        );
    }

    void clear() {
        evidenceStore.clear();
    }
}
