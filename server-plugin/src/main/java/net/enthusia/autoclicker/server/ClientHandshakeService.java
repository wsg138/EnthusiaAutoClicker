package net.enthusia.autoclicker.server;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            int protocol = input.readUnsignedByte();
            if (protocol != 1) {
                return null;
            }
            String modVersion = trim(input.readUTF(), 64);
            String loader = trim(input.readUTF(), 32);
            String minecraftVersion = trim(input.readUTF(), 32);
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
}
