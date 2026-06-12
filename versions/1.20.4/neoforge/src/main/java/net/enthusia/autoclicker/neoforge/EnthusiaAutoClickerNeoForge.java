package net.enthusia.autoclicker.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.client.AutoclickerRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

@Mod(EnthusiaAutoClickerNeoForge.MOD_ID)
@Mod.EventBusSubscriber(
    modid = EnthusiaAutoClickerNeoForge.MOD_ID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class EnthusiaAutoClickerNeoForge {
    public static final String MOD_ID = "enthusia_autoclicker";
    private static final String CATEGORY_KEY = "key.category.enthusia_autoclicker.main";
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

    public EnthusiaAutoClickerNeoForge(ModContainer container) {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("enthusia-autoclicker.properties");
        AutoclickerConfig config = AutoclickerConfig.load(configPath);
        runtime = new AutoclickerRuntime(config, TOGGLE_KEY, SETTINGS_KEY);
        container.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, parent) -> new net.enthusia.autoclicker.client.AutoclickerSettingsScreen(config, parent)
            )
        );
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY);
        event.register(SETTINGS_KEY);
    }

    @SubscribeEvent
    public static void onClientPreTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && runtime != null) {
            runtime.preTick(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && runtime != null) {
            runtime.tick(Minecraft.getInstance());
        }
    }
}
