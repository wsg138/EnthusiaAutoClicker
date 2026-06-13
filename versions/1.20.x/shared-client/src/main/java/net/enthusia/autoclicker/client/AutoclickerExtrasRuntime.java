package net.enthusia.autoclicker.client;

import net.enthusia.autoclicker.AutoclickerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

final class AutoclickerExtrasRuntime {
    private static final long ARMOR_STAND_EATING_DELAY_MILLIS = 10_000L;
    private static final long INVENTORY_CHECK_INTERVAL_MILLIS = 1_000L;

    private final AutoclickerConfig config;
    private long nextInventoryCheckMillis;

    AutoclickerExtrasRuntime(AutoclickerConfig config) {
        this.config = config;
    }

    TickResult tick(Minecraft client, long nowMillis) {
        String durabilityStop = lowDurabilityStopReason(client);
        if (durabilityStop != null) {
            return TickResult.stop(durabilityStop);
        }
        return manageFoodInventory(client, nowMillis);
    }

    boolean canEatThroughTarget(Minecraft client, long enabledForMillis) {
        if (!(client.hitResult instanceof EntityHitResult entityHit)
            || !(entityHit.getEntity() instanceof ArmorStand)) {
            return true;
        }
        return config.armorStandEating()
            && enabledForMillis >= ARMOR_STAND_EATING_DELAY_MILLIS;
    }

    private String lowDurabilityStopReason(Minecraft client) {
        if (!config.durabilityGuard() || !config.leftEnabled() || client.player == null) {
            return null;
        }

        ItemStack heldItem = client.player.getMainHandItem();
        if (!heldItem.isDamageableItem()) {
            return null;
        }

        int remaining = heldItem.getMaxDamage() - heldItem.getDamageValue();
        return remaining <= config.minimumDurability()
            ? "Stopped: held item has " + remaining + " durability remaining"
            : null;
    }

    private TickResult manageFoodInventory(Minecraft client, long nowMillis) {
        if (!config.leftEnabled()
            || !config.foodEnabled()
            || client.player == null
            || client.gameMode == null
            || client.player.isUsingItem()
            || nowMillis < nextInventoryCheckMillis) {
            return TickResult.NONE;
        }
        nextInventoryCheckMillis = nowMillis + INVENTORY_CHECK_INTERVAL_MILLIS;

        ItemStack offhand = client.player.getOffhandItem();
        if (config.autoRestock()
            && canReplaceOffhand(offhand)
            && offhand.getCount() <= config.restockAtCount()) {
            int sourceSlot = findFoodSlot(client.player.getInventory(), offhand);
            if (sourceSlot >= 0 && restockOffhand(client, sourceSlot)) {
                return TickResult.PAUSE;
            }
        }

        if (config.stopWhenOutOfFood()
            && client.player.getFoodData().getFoodLevel() <= config.foodLevelThreshold()
            && !AutoclickerRuntime.isFood(offhand)) {
            return TickResult.stop("Stopped: no usable offhand food");
        }
        return TickResult.NONE;
    }

    private static boolean canReplaceOffhand(ItemStack offhand) {
        return offhand.isEmpty() || AutoclickerRuntime.isFood(offhand);
    }

    private static int findFoodSlot(Inventory inventory, ItemStack offhand) {
        int matchingSlot = offhand.getCount() < offhand.getMaxStackSize()
            ? findLargestFoodStack(inventory, offhand, true)
            : -1;
        return matchingSlot >= 0 ? matchingSlot : findLargestFoodStack(inventory, offhand, false);
    }

    private static int findLargestFoodStack(Inventory inventory, ItemStack offhand, boolean matchingOnly) {
        int bestSlot = -1;
        int bestCount = matchingOnly ? 0 : offhand.getCount();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!AutoclickerRuntime.isFood(candidate) || candidate.getCount() <= bestCount) {
                continue;
            }
            if (matchingOnly
                && AutoclickerRuntime.isFood(offhand)
                && !ItemStack.isSameItemSameTags(candidate, offhand)) {
                continue;
            }
            bestSlot = slot;
            bestCount = candidate.getCount();
        }
        return bestSlot;
    }

    private static boolean restockOffhand(Minecraft client, int inventorySlot) {
        if (client.player == null
            || client.gameMode == null
            || client.player.containerMenu != client.player.inventoryMenu
            || !client.player.containerMenu.getCarried().isEmpty()) {
            return false;
        }

        int menuSlot = inventorySlot < Inventory.getSelectionSize()
            ? InventoryMenu.USE_ROW_SLOT_START + inventorySlot
            : inventorySlot;
        ItemStack source = client.player.getInventory().getItem(inventorySlot);
        ItemStack offhand = client.player.getOffhandItem();
        if (!offhand.isEmpty() && ItemStack.isSameItemSameTags(source, offhand)) {
            clickInventorySlot(client, menuSlot, 0, ClickType.PICKUP);
            clickInventorySlot(client, InventoryMenu.SHIELD_SLOT, 0, ClickType.PICKUP);
            clickInventorySlot(client, menuSlot, 0, ClickType.PICKUP);
            return true;
        }

        clickInventorySlot(client, menuSlot, Inventory.SLOT_OFFHAND, ClickType.SWAP);
        return true;
    }

    private static void clickInventorySlot(
        Minecraft client,
        int menuSlot,
        int button,
        ClickType clickType
    ) {
        client.gameMode.handleInventoryMouseClick(
            InventoryMenu.CONTAINER_ID,
            menuSlot,
            button,
            clickType,
            client.player
        );
    }

    record TickResult(boolean pauseTick, String stopReason) {
        static final TickResult NONE = new TickResult(false, null);
        static final TickResult PAUSE = new TickResult(true, null);

        static TickResult stop(String reason) {
            return new TickResult(true, reason);
        }
    }
}
