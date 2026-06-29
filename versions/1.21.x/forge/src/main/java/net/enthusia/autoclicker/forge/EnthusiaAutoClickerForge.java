package net.enthusia.autoclicker.forge;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.client.AutoclickerRuntime;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import org.lwjgl.glfw.GLFW;

@Mod(EnthusiaAutoClickerForge.MOD_ID)
public final class EnthusiaAutoClickerForge {
    public static final String MOD_ID = "enthusia_autoclicker";
    private static final Identifier HANDSHAKE_CHANNEL = Identifier.fromNamespaceAndPath(MOD_ID, "handshake");
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );
    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
        "key.enthusia_autoclicker.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        CATEGORY
    );
    private static final KeyMapping SETTINGS_KEY = new KeyMapping(
        "key.enthusia_autoclicker.settings",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        CATEGORY
    );
    private static AutoclickerRuntime runtime;
    private static String modVersion = "unknown";
    private static final Channel<CustomPacketPayload> HANDSHAKE_NETWORK = ChannelBuilder.named(HANDSHAKE_CHANNEL)
        .optional()
        .payloadChannel()
        .play()
        .serverbound()
        .add(HandshakePayload.TYPE, HandshakePayload.CODEC, (payload, context) -> {})
        .build();

    public EnthusiaAutoClickerForge(ModContainer container) {
        modVersion = container.getModInfo().getVersion().toString();
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("enthusia-autoclicker.properties");
        AutoclickerConfig config = AutoclickerConfig.load(configPath);
        runtime = new AutoclickerRuntime(config, TOGGLE_KEY, SETTINGS_KEY);
        container.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                parent -> new net.enthusia.autoclicker.client.AutoclickerSettingsScreen(config, parent)
            )
        );
        RegisterKeyMappingsEvent.BUS.addListener(EnthusiaAutoClickerForge::registerKeyMappings);
        TickEvent.ClientTickEvent.Pre.BUS.addListener(EnthusiaAutoClickerForge::onClientPreTick);
        TickEvent.ClientTickEvent.Post.BUS.addListener(EnthusiaAutoClickerForge::onClientTick);
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(EnthusiaAutoClickerForge::onClientLogin);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY);
        event.register(SETTINGS_KEY);
    }

    private static void onClientPreTick(TickEvent.ClientTickEvent.Pre event) {
        if (runtime != null) {
            runtime.preTick(Minecraft.getInstance());
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        if (runtime != null) {
            runtime.tick(Minecraft.getInstance());
        }
    }

    private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        HANDSHAKE_NETWORK.send(new HandshakePayload(
            1,
            modVersion,
            "forge",
            SharedConstants.getCurrentVersion().name()
        ), event.getConnection());
    }

    private record HandshakePayload(
        int protocolVersion,
        String modVersion,
        String loader,
        String minecraftVersion
    ) implements CustomPacketPayload {
        private static final CustomPacketPayload.Type<HandshakePayload> TYPE =
            new CustomPacketPayload.Type<>(HANDSHAKE_CHANNEL);
        private static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC =
            CustomPacketPayload.codec(HandshakePayload::write, HandshakePayload::read);

        private static HandshakePayload read(FriendlyByteBuf buffer) {
            return new HandshakePayload(buffer.readByte(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeByte(protocolVersion);
            buffer.writeUtf(modVersion);
            buffer.writeUtf(loader);
            buffer.writeUtf(minecraftVersion);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
