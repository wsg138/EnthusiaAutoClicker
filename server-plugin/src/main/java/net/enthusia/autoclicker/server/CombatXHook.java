package net.enthusia.autoclicker.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class CombatXHook {
    private static final String COMBAT_MANAGER_CLASS = "net.sparkomc.combatx.CombatManager";
    private static final String COMBAT_MANAGER_RESOURCE = "net/sparkomc/combatx/CombatManager.class";

    private Plugin owner;
    private Method isInCombatMethod;
    private boolean available;
    private boolean warnedUnavailable;

    boolean initialize(Plugin plugin) {
        owner = plugin;
        return refresh();
    }

    boolean refresh() {
        if (available) {
            return true;
        }
        Plugin combatX = findCombatXPlugin();
        if (combatX != null && hookFromPlugin(combatX)) {
            return true;
        }
        if (hookFromCurrentClassLoader()) {
            owner.getLogger().info("Hooked CombatX from the shared server classloader.");
            return true;
        }
        available = false;
        if (!warnedUnavailable) {
            owner.getLogger().warning("CombatX API could not be found. Autoclicking will stay blocked.");
            owner.getLogger().warning("Loaded plugins: " + loadedPluginNames());
            warnedUnavailable = true;
        }
        return false;
    }

    boolean isAvailable() {
        return available || refresh();
    }

    boolean isInCombat(Player player) {
        if (!isAvailable()) {
            return true;
        }
        try {
            Object result = isInCombatMethod.invoke(null, player);
            return result instanceof Boolean combat && combat;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return true;
        }
    }

    private Plugin findCombatXPlugin() {
        Plugin exact = owner.getServer().getPluginManager().getPlugin("CombatX");
        if (exact != null && exact.isEnabled()) {
            return exact;
        }
        Plugin named = null;
        for (Plugin candidate : owner.getServer().getPluginManager().getPlugins()) {
            if (!candidate.isEnabled()) {
                continue;
            }
            String lowerName = candidate.getName().toLowerCase();
            if (lowerName.contains("combatx")) {
                named = candidate;
            }
            URL apiResource = candidate.getClass().getClassLoader().getResource(COMBAT_MANAGER_RESOURCE);
            if (apiResource != null) {
                return candidate;
            }
        }
        return named;
    }

    private boolean hookFromPlugin(Plugin plugin) {
        try {
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            Class<?> combatManager = Class.forName(COMBAT_MANAGER_CLASS, true, classLoader);
            isInCombatMethod = combatManager.getMethod("isInCombat", Player.class);
            available = true;
            warnedUnavailable = false;
            owner.getLogger().info("Hooked CombatX via plugin " + plugin.getName() + ".");
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return false;
        }
    }

    private boolean hookFromCurrentClassLoader() {
        try {
            Class<?> combatManager = Class.forName(COMBAT_MANAGER_CLASS);
            isInCombatMethod = combatManager.getMethod("isInCombat", Player.class);
            available = true;
            warnedUnavailable = false;
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return false;
        }
    }

    private String loadedPluginNames() {
        StringBuilder names = new StringBuilder();
        for (Plugin plugin : owner.getServer().getPluginManager().getPlugins()) {
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(plugin.getName());
        }
        return names.toString();
    }
}
