package net.enthusia.autoclicker.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.AutoclickerEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class AutoclickerRuntime {
    private final AutoclickerConfig config;
    private final AutoclickerEngine engine = new AutoclickerEngine();
    private final KeyMapping toggleKey;
    private final KeyMapping settingsKey;
    private boolean leftApplied;
    private boolean rightApplied;

    public AutoclickerRuntime(
        AutoclickerConfig config,
        KeyMapping toggleKey,
        KeyMapping settingsKey
    ) {
        this.config = config;
        this.toggleKey = toggleKey;
        this.settingsKey = settingsKey;
    }

    public void tick(Minecraft client) {
        releaseAppliedKeys(client);

        while (toggleKey.consumeClick()) {
            engine.toggle(config, now());
            showStatus(client, engine.isEnabled() ? "Enabled" : "Disabled");
        }

        while (settingsKey.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(new AutoclickerSettingsScreen(config, null));
            }
        }

        if (engine.updateTimer(now())) {
            showStatus(client, "Timed run complete");
            return;
        }

        boolean safe = client.player != null
            && client.level != null
            && client.gameMode != null
            && client.screen == null;
        boolean usingItem = client.player != null && client.player.isUsingItem();
        AutoclickerEngine.TickDecision decision = engine.decide(config, now(), safe, usingItem);

        if (decision.clickRight()) {
            KeyMapping.click(boundKey(client.options.keyUse));
        }
        if (decision.clickLeft()) {
            KeyMapping.click(boundKey(client.options.keyAttack));
        }
        if (decision.holdRight()) {
            client.options.keyUse.setDown(true);
            rightApplied = true;
        }
        if (decision.holdLeft()) {
            client.options.keyAttack.setDown(true);
            leftApplied = true;
        }
    }

    public void stop(Minecraft client) {
        engine.setEnabled(false, config, now());
        releaseAppliedKeys(client);
    }

    private void releaseAppliedKeys(Minecraft client) {
        if (leftApplied) {
            restorePhysicalState(client, client.options.keyAttack);
            leftApplied = false;
        }
        if (rightApplied) {
            restorePhysicalState(client, client.options.keyUse);
            rightApplied = false;
        }
    }

    private static void restorePhysicalState(Minecraft client, KeyMapping mapping) {
        InputConstants.Key key = boundKey(mapping);
        boolean physicallyDown = switch (key.getType()) {
            case MOUSE -> GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
            case KEYSYM -> InputConstants.isKeyDown(client.getWindow(), key.getValue());
            case SCANCODE -> false;
        };
        mapping.setDown(physicallyDown);
    }

    private static InputConstants.Key boundKey(KeyMapping mapping) {
        return InputConstants.getKey(mapping.saveString());
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    private static void showStatus(Minecraft client, String status) {
        if (client.player != null) {
            client.player.displayClientMessage(
                Component.literal("[Enthusia AutoClicker] ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(status).withStyle(ChatFormatting.GRAY)),
                true
            );
        }
    }
}
