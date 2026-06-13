package net.enthusia.autoclicker.forge;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.client.AutoclickerRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

@Mod(EnthusiaAutoClickerForge.MOD_ID)
public final class EnthusiaAutoClickerForge {
    public static final String MOD_ID = "enthusia_autoclicker";
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

    public EnthusiaAutoClickerForge(ModContainer container) {
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
}
