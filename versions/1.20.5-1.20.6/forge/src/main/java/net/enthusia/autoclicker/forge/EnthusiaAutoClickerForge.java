package net.enthusia.autoclicker.forge;

import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.Unpooled;
import java.nio.file.Path;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.client.AutoclickerRuntime;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

@Mod(EnthusiaAutoClickerForge.MOD_ID)
public final class EnthusiaAutoClickerForge {
    public static final String MOD_ID = "enthusia_autoclicker";
    private static final String CATEGORY_KEY = "key.category.enthusia_autoclicker.main";
    private static final ResourceLocation HANDSHAKE_CHANNEL = new ResourceLocation(MOD_ID, "handshake");

    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
        "key.enthusia_autoclicker.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        CATEGORY_KEY
    );
    private static final KeyMapping SETTINGS_KEY = new KeyMapping(
        "key.enthusia_autoclicker.settings",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        CATEGORY_KEY
    );
    private static AutoclickerRuntime runtime;

    public EnthusiaAutoClickerForge() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("enthusia-autoclicker.properties");
        AutoclickerConfig config = AutoclickerConfig.load(configPath);
        runtime = new AutoclickerRuntime(config, TOGGLE_KEY, SETTINGS_KEY);

        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (parent) -> new net.enthusia.autoclicker.client.AutoclickerSettingsScreen(config, parent)
            )
        );

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterKeys);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (runtime != null) {
                runtime.preTick(Minecraft.getInstance());
            }
        } else if (event.phase == TickEvent.Phase.END) {
            if (runtime != null) {
                runtime.tick(Minecraft.getInstance());
            }
        }
    }

    @SubscribeEvent
    public void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeByte(1);
        buffer.writeUtf(ModList.get().getModContainerById(MOD_ID)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown"));
        buffer.writeUtf("forge");
        buffer.writeUtf(SharedConstants.getCurrentVersion().getName());
        event.getConnection().send(new ServerboundCustomPayloadPacket(new HandshakePayload(buffer)));
    }

    private record HandshakePayload(FriendlyByteBuf data) implements CustomPacketPayload {
        private static final CustomPacketPayload.Type<HandshakePayload> TYPE =
            new CustomPacketPayload.Type<>(HANDSHAKE_CHANNEL);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY);
        event.register(SETTINGS_KEY);
    }
}
