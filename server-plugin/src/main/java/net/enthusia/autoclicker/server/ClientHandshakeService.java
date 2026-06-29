package net.enthusia.autoclicker.server;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

final class ClientHandshakeService implements PluginMessageListener {
    static final String CHANNEL = "enthusia_autoclicker:handshake";

    private final Map<UUID, ClientHandshake> handshakes = new HashMap<>();

    @Override
    public void onPluginMessageReceived(
        @NotNull String channel,
        @NotNull Player player,
        byte @NotNull [] message
    ) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        ClientHandshake handshake = parse(message);
        if (handshake != null) {
            handshakes.put(player.getUniqueId(), handshake);
        }
    }

    Optional<ClientHandshake> handshake(Player player) {
        return Optional.ofNullable(handshakes.get(player.getUniqueId()));
    }

    void forget(Player player) {
        handshakes.remove(player.getUniqueId());
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
            return new ClientHandshake(modVersion, loader, minecraftVersion, Instant.now());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String trim(String value, int maxLength) {
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
