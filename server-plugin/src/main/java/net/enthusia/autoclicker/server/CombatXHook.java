package net.enthusia.autoclicker.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class CombatXHook {
    private Method isInCombatMethod;
    private boolean available;

    boolean initialize(Plugin plugin) {
        Plugin combatX = plugin.getServer().getPluginManager().getPlugin("CombatX");
        if (combatX == null || !combatX.isEnabled()) {
            available = false;
            return false;
        }
        try {
            Class<?> combatManager = Class.forName("net.sparkomc.combatx.CombatManager");
            isInCombatMethod = combatManager.getMethod("isInCombat", Player.class);
            available = true;
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            available = false;
            plugin.getLogger().warning("CombatX is installed, but its CombatManager API could not be found.");
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
}
