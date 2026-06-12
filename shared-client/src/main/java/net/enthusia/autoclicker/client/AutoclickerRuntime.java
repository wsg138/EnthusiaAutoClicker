package net.enthusia.autoclicker.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.enthusia.autoclicker.AutoclickerConfig;
import net.enthusia.autoclicker.AutoclickerEngine;
import net.enthusia.autoclicker.DurationParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public final class AutoclickerRuntime {
    private final AutoclickerConfig config;
    private final AutoclickerEngine engine = new AutoclickerEngine();
    private final AutoclickerExtrasRuntime extras;
    private final KeyMapping toggleKey;
    private final KeyMapping settingsKey;
    private boolean leftApplied;
    private boolean rightApplied;
    private long nextFoodAttemptMillis;
    private long continuousUseStartedMillis;

    public AutoclickerRuntime(
        AutoclickerConfig config,
        KeyMapping toggleKey,
        KeyMapping settingsKey
    ) {
        this.config = config;
        this.extras = new AutoclickerExtrasRuntime(config);
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

        long nowMillis = now();
        if (engine.updateTimer(nowMillis)) {
            showStatus(client, "Timed run complete");
            return;
        }

        boolean inWorld = client.player != null
            && client.level != null
            && client.gameMode != null;
        if (!inWorld && engine.isEnabled()) {
            engine.setEnabled(false, config, nowMillis);
            continuousUseStartedMillis = 0L;
            return;
        }

        boolean safe = inWorld && client.screen == null;
        updateContinuousUseTimer(safe, nowMillis);
        if (safe && engine.isEnabled()) {
            AutoclickerExtrasRuntime.TickResult extrasResult = extras.tick(client, nowMillis);
            if (extrasResult.stopReason() != null) {
                engine.setEnabled(false, config, nowMillis);
                showStatus(client, extrasResult.stopReason());
            }
            if (extrasResult.pauseTick()) {
                return;
            }
        }
        boolean usingItem = client.player != null && client.player.isUsingItem();
        boolean foodActive = safe && shouldEatOffhand(client, nowMillis);
        boolean playerTargeted = safe && isTargetingPlayer(client);
        AutoclickerEngine.TickDecision decision = engine.decide(
            config,
            nowMillis,
            safe,
            usingItem,
            foodActive,
            playerTargeted
        );

        if (decision.holdFood()) {
            startOrContinueEating(client);
        } else if (decision.clickRight()) {
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

    public void preTick(Minecraft client) {
        if (!engine.isEnabled() || !isTargetingPlayer(client)) {
            return;
        }
        if (leftApplied) {
            client.options.keyAttack.setDown(false);
        }
        if (rightApplied) {
            client.options.keyUse.setDown(false);
        }
        while (client.options.keyAttack.consumeClick()) {
            // Remove queued automated input before vanilla handles the player target.
        }
        while (client.options.keyUse.consumeClick()) {
            // Remove queued automated input before vanilla handles the player target.
        }
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

    private boolean shouldEatOffhand(Minecraft client, long nowMillis) {
        if (!config.leftEnabled() || !config.foodEnabled() || client.player == null) {
            return false;
        }

        ItemStack offhand = client.player.getOffhandItem();
        var food = offhand.get(DataComponents.FOOD);
        boolean offhandFood = food != null && offhand.has(DataComponents.CONSUMABLE);
        if (!offhandFood) {
            return false;
        }

        if (client.player.isUsingItem()) {
            return client.player.getUsedItemHand() == InteractionHand.OFF_HAND;
        }
        boolean readyToEat = client.player.getFoodData().getFoodLevel() <= config.foodLevelThreshold()
            && client.player.canEat(food.canAlwaysEat())
            && !client.player.getCooldowns().isOnCooldown(offhand);
        if (!readyToEat) {
            return false;
        }
        long continuousUseMillis = continuousUseStartedMillis == 0L
            ? 0L
            : nowMillis - continuousUseStartedMillis;
        return extras.canEatThroughTarget(client, continuousUseMillis);
    }

    private void startOrContinueEating(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        long nowMillis = now();
        if (!client.player.isUsingItem() && nowMillis >= nextFoodAttemptMillis) {
            nextFoodAttemptMillis = nowMillis + DurationParser.MINIMUM_CLICK_INTERVAL_MILLIS;
            client.gameMode.useItem(client.player, InteractionHand.OFF_HAND);
        }
        if (client.player.isUsingItem() && client.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            client.options.keyUse.setDown(true);
            rightApplied = true;
        }
    }

    private void showStatus(Minecraft client, String status) {
        if (config.statusMessages() && client.player != null) {
            client.player.displayClientMessage(
                Component.literal("[Enthusia AutoClicker] ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(status).withStyle(ChatFormatting.GRAY)),
                true
            );
        }
    }

    private void updateContinuousUseTimer(boolean safe, long nowMillis) {
        if (!engine.isEnabled() || !safe) {
            continuousUseStartedMillis = 0L;
        } else if (continuousUseStartedMillis == 0L) {
            continuousUseStartedMillis = nowMillis;
        }
    }

    private static boolean isTargetingPlayer(Minecraft client) {
        return client.hitResult instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof Player;
    }

}
