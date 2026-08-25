package net.enthusia.autoclicker.server;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.enthusia.autoclicker.server.api.ClientHandshakeSnapshot;
import net.enthusia.autoclicker.server.api.EnthusiaAutoClickerClientApi;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

final class ClientHandshakeService implements PluginMessageListener, EnthusiaAutoClickerClientApi {
    static final String CHANNEL = "enthusia_autoclicker:handshake";

    private final Map<UUID, ClientHandshake> handshakes = new ConcurrentHashMap<>();
    private final Clock clock;

    ClientHandshakeService() {
        this(Clock.systemUTC());
    }

    ClientHandshakeService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void onPluginMessageReceived(
        @NotNull String channel,
        @NotNull Player player,
        byte @NotNull [] message
    ) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        accept(player.getUniqueId(), message);
    }

    void accept(UUID playerId, byte[] message) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(message, "message");
        ClientHandshake handshake = parse(message);
        if (handshake != null) {
            handshakes.put(playerId, handshake);
        } else {
            handshakes.remove(playerId);
        }
    }

    Optional<ClientHandshake> handshake(Player player) {
        Objects.requireNonNull(player, "player");
        return Optional.ofNullable(handshakes.get(player.getUniqueId()));
    }

    @Override
    public Optional<ClientHandshakeSnapshot> handshake(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return Optional.ofNullable(handshakes.get(playerId)).map(value -> new ClientHandshakeSnapshot(
            value.modVersion(),
            value.loader(),
            value.minecraftVersion(),
            value.receivedAt()
        ));
    }

    void forget(Player player) {
        Objects.requireNonNull(player, "player");
        handshakes.remove(player.getUniqueId());
    }

    void clear() {
        handshakes.clear();
    }

    private ClientHandshake parse(byte[] message) {
        try {
            Cursor cursor = new Cursor(message);
            int protocol = cursor.readUnsignedByte();
            if (protocol != 1) {
                return null;
            }
            String modVersion = trim(cursor.readUtf(64), 64);
            String loader = trim(cursor.readUtf(32), 32);
            String minecraftVersion = trim(cursor.readUtf(32), 32);
            return new ClientHandshake(modVersion, loader, minecraftVersion, clock.instant());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static final class Cursor {
        private final byte[] data;
        private int index;

        private Cursor(byte[] data) {
            this.data = data;
        }

        private int readUnsignedByte() {
            if (index >= data.length) {
                throw new IllegalArgumentException("Unexpected end of handshake payload");
            }
            return data[index++] & 0xFF;
        }

        private String readUtf(int maxCharacters) {
            int length = readVarInt();
            int maxBytes = maxCharacters * 4;
            if (length < 0 || length > maxBytes || index + length > data.length) {
                throw new IllegalArgumentException("Invalid handshake string length");
            }
            String value = new String(data, index, length, StandardCharsets.UTF_8);
            index += length;
            return value;
        }

        private int readVarInt() {
            int value = 0;
            int position = 0;
            while (position < 32) {
                int current = readUnsignedByte();
                value |= (current & 0x7F) << position;
                if ((current & 0x80) == 0) {
                    return value;
                }
                position += 7;
            }
            throw new IllegalArgumentException("VarInt is too large");
        }
    }
}
