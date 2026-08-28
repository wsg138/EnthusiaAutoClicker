package net.enthusia.autoclicker.server;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.enthusia.autoclicker.server.api.ClientEvidenceSnapshot;
import net.enthusia.autoclicker.server.api.EnthusiaAutoClickerClientApi;

final class ClientEvidenceStore {
    private final Object lock = new Object();
    private final Map<UUID, ClientEvidenceSnapshot> evidenceByPlayer = new LinkedHashMap<>();
    private ClientEvidencePolicy policy;

    ClientEvidenceStore(ClientEvidencePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    void store(ClientEvidenceSnapshot evidence, Instant now) {
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(now, "now");
        synchronized (lock) {
            pruneExpired(now);
            evidenceByPlayer.remove(evidence.playerId());
            evidenceByPlayer.put(evidence.playerId(), evidence);
            trimToMaximum();
        }
    }

    ClientEvidenceSnapshot find(UUID playerId, Instant now) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(now, "now");
        synchronized (lock) {
            pruneExpired(now);
            ClientEvidenceSnapshot evidence = evidenceByPlayer.get(playerId);
            return evidence == null
                    ? ClientEvidenceSnapshot.notObserved(
                            playerId,
                            EnthusiaAutoClickerClientApi.EVIDENCE_VERSION
                    )
                    : evidence;
        }
    }

    void markOffline(UUID playerId, Instant now) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(now, "now");
        synchronized (lock) {
            pruneExpired(now);
            evidenceByPlayer.computeIfPresent(
                    playerId,
                    (ignored, evidence) -> evidence.asPreviousSession()
            );
        }
    }

    void updatePolicy(ClientEvidencePolicy policy, Instant now) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(now, "now");
        synchronized (lock) {
            this.policy = policy;
            pruneExpired(now);
            trimToMaximum();
        }
    }

    void clear() {
        synchronized (lock) {
            evidenceByPlayer.clear();
        }
    }

    private void pruneExpired(Instant now) {
        Instant cutoff = now.minus(policy.retention());
        evidenceByPlayer.values().removeIf(evidence -> !evidence.observedAt()
                .orElseThrow()
                .isAfter(cutoff));
    }

    private void trimToMaximum() {
        Iterator<UUID> players = evidenceByPlayer.keySet().iterator();
        while (evidenceByPlayer.size() > policy.maximumRecords() && players.hasNext()) {
            players.next();
            players.remove();
        }
    }
}
