package net.enthusia.autoclicker.client;

import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.AutoclickerEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
                client.setScreen(new AutoclickerSettingsScreen(config));
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

        boolean applyRight = decision.holdRight() || decision.clickRight();
        boolean applyLeft = decision.holdLeft() || decision.clickLeft();
        if (applyRight) {
            client.options.keyUse.setDown(true);
            rightApplied = true;
        }
        if (applyLeft) {
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
            client.options.keyAttack.setDown(false);
            leftApplied = false;
        }
        if (rightApplied) {
            client.options.keyUse.setDown(false);
            rightApplied = false;
        }
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
