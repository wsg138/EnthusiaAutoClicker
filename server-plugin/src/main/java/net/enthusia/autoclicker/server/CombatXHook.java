package net.enthusia.autoclicker.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class CombatXHook {
    private Method isInCombatMethod;
    private boolean available;

    boolean initialize(Plugin plugin) {
        try {
            Class<?> combatManager = Class.forName("net.sparkomc.combatx.CombatManager");
            isInCombatMethod = combatManager.getMethod("isInCombat", Player.class);
            available = true;
            Plugin combatX = findCombatXPlugin(plugin);
            if (combatX == null) {
                plugin.getLogger().warning("CombatX API was found, but no enabled CombatX plugin entry was found.");
            } else {
                plugin.getLogger().info("Hooked CombatX via plugin " + combatX.getName() + ".");
            }
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            available = false;
            plugin.getLogger().warning("CombatX API could not be found. Autoclicking will stay blocked.");
            return false;
        }
    }

    boolean isAvailable() {
        return available;
    }

    boolean isInCombat(Player player) {
        if (!available) {
            return true;
        }
        try {
            Object result = isInCombatMethod.invoke(null, player);
            return result instanceof Boolean combat && combat;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return true;
        }
    }

    private Plugin findCombatXPlugin(Plugin plugin) {
        Plugin exact = plugin.getServer().getPluginManager().getPlugin("CombatX");
        if (exact != null && exact.isEnabled()) {
            return exact;
        }
        return Arrays.stream(plugin.getServer().getPluginManager().getPlugins())
            .filter(candidate -> candidate.isEnabled()
                && candidate.getName().toLowerCase().contains("combatx"))
            .findFirst()
            .orElse(null);
    }
}
