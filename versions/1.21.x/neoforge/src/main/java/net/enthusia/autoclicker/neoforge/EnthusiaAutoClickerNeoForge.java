package net.enthusia.autoclicker.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.client.AutoclickerRuntime;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = EnthusiaAutoClickerNeoForge.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = EnthusiaAutoClickerNeoForge.MOD_ID, value = Dist.CLIENT)
public final class EnthusiaAutoClickerNeoForge {
    public static final String MOD_ID = "enthusia_autoclicker";
    private static final Identifier HANDSHAKE_CHANNEL = Identifier.fromNamespaceAndPath(MOD_ID, "handshake");
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
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
    private static final AtomicReference<BootstrapState> BOOTSTRAP_STATE = new AtomicReference<>();

    public EnthusiaAutoClickerNeoForge(ModContainer container) {
        String modVersion = container.getModInfo().getVersion().toString();
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("enthusia-autoclicker.properties");
        AutoclickerConfig config = AutoclickerConfig.load(configPath);
        AutoclickerRuntime runtime = new AutoclickerRuntime(config, TOGGLE_KEY, SETTINGS_KEY);
        BOOTSTRAP_STATE.set(new BootstrapState(runtime, modVersion));
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (ignored, parent) -> new net.enthusia.autoclicker.client.AutoclickerSettingsScreen(config, parent)
        );
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(MOD_ID)
            .optional()
            .playToServer(HandshakePayload.TYPE, HandshakePayload.CODEC, (payload, context) -> {});
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_KEY);
        event.register(SETTINGS_KEY);
    }

    @SubscribeEvent
    public static void onClientPreTick(ClientTickEvent.Pre event) {
        BootstrapState state = BOOTSTRAP_STATE.get();
        if (state != null) {
            state.runtime().preTick(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BootstrapState state = BOOTSTRAP_STATE.get();
        if (state != null) {
            state.runtime().tick(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        BootstrapState state = BOOTSTRAP_STATE.get();
        event.getConnection().send(new ServerboundCustomPayloadPacket(new HandshakePayload(
            1,
            state == null ? "unknown" : state.modVersion(),
            "neoforge",
            SharedConstants.getCurrentVersion().name()
        )));
    }

    private record BootstrapState(AutoclickerRuntime runtime, String modVersion) {}

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
