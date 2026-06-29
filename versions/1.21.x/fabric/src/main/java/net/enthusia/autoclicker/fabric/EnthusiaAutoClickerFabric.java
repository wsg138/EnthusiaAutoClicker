package net.enthusia.autoclicker.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.client.AutoclickerRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EnthusiaAutoClickerFabric implements ClientModInitializer {
    private static final Identifier HANDSHAKE_CHANNEL = Identifier.fromNamespaceAndPath(
        "enthusia_autoclicker",
        "handshake"
    );
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("enthusia_autoclicker", "main")
    );

    private static AutoclickerConfig config;
    private AutoclickerRuntime runtime;

    @Override
    public void onInitializeClient() {
        KeyMapping toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.enthusia_autoclicker.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        ));
        KeyMapping settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.enthusia_autoclicker.settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
        ));

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("enthusia-autoclicker.properties");
        config = AutoclickerConfig.load(configPath);
        runtime = new AutoclickerRuntime(config, toggleKey, settingsKey);
        ClientTickEvents.START_CLIENT_TICK.register(runtime::preTick);
        ClientTickEvents.END_CLIENT_TICK.register(runtime::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(runtime::stop);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendHandshake());
    }

    public static Screen createConfigScreen(Screen parent) {
        if (config == null) {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("enthusia-autoclicker.properties");
            config = AutoclickerConfig.load(configPath);
        }
        return new net.enthusia.autoclicker.client.AutoclickerSettingsScreen(config, parent);
    }

    private static void sendHandshake() {
        if (!Boolean.getBoolean("enthusia.autoclicker.handshake")) {
            return;
        }
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(1);
        buffer.writeUtf(FabricLoader.getInstance().getModContainer("enthusia_autoclicker")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown"));
        buffer.writeUtf("fabric");
        buffer.writeUtf(FabricLoader.getInstance().getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown"));
        ClientPlayNetworking.send(new HandshakePayload(buffer));
    }

    private record HandshakePayload(FriendlyByteBuf data) implements CustomPacketPayload {
        private static final CustomPacketPayload.Type<HandshakePayload> TYPE =
            new CustomPacketPayload.Type<>(HANDSHAKE_CHANNEL);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
