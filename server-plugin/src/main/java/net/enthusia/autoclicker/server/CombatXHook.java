package net.enthusia.autoclicker.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

final class CombatXHook implements Listener {
    private static final long RETRY_INTERVAL_NANOS = 30_000_000_000L;
    private static final String COMBAT_MANAGER_CLASS = "net.sparkomc.combatx.CombatManager";
    private static final String COMBAT_MANAGER_RESOURCE = "net/sparkomc/combatx/CombatManager.class";

    private final LongSupplier nanoTime;
    private final BooleanSupplier discovery;

    CombatXHook() {
        nanoTime = System::nanoTime;
        discovery = () -> discover();
    }

    CombatXHook(LongSupplier nanoTime, BooleanSupplier discovery) {
        this.nanoTime = nanoTime;
        this.discovery = discovery;
    }

    private EnthusiaServerAutoClickerPlugin owner;
    private Method isInCombatMethod;
    private boolean available;
    private boolean attempted;
    private long lastAttemptNanos;
    private boolean warnedUnavailable;

    boolean initialize(EnthusiaServerAutoClickerPlugin plugin) {
        owner = plugin;
        return refresh();
    }

    void resetAvailabilityWarning() {
        warnedUnavailable = false;
    }

    boolean refresh() {
        invalidate();
        return isAvailable();
    }

    void invalidate() {
        available = false;
        attempted = false;
        isInCombatMethod = null;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        invalidate();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        invalidate();
    }

    private boolean discover() {
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
            if (owner.requireCombatX()) {
                owner.getLogger().warning("CombatX API could not be found. Autoclicking will stay blocked.");
            } else {
                owner.getLogger().info("CombatX API could not be found. Internal PvP fallback tracking is active.");
            }
            owner.getLogger().warning("Loaded plugins: " + loadedPluginNames());
            warnedUnavailable = true;
        }
        return false;
    }

    boolean isAvailable() {
        if (available) {
            return true;
        }
        long now = nanoTime.getAsLong();
        if (attempted && now - lastAttemptNanos < RETRY_INTERVAL_NANOS) {
            return false;
        }
        attempted = true;
        lastAttemptNanos = now;
        available = discovery.getAsBoolean();
        return available;
    }

    boolean isInCombat(Player player) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Object result = isInCombatMethod.invoke(null, player);
            return result instanceof Boolean combat && combat;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            owner.getLogger().warning("CombatX combat check failed for " + player.getName() + ". Blocking autoclicking for this tick.");
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
            String lowerName = candidate.getName().toLowerCase(Locale.ROOT);
            if (lowerName.contains("combatx")) {
                named = candidate;
            }
            URL apiResource = candidate.getClass().getClassLoader() // NOPMD - Query the supplying plugin's loader.
                .getResource(COMBAT_MANAGER_RESOURCE);
            if (apiResource != null) {
                return candidate;
            }
        }
        return named;
    }

    private boolean hookFromPlugin(Plugin plugin) {
        try {
            ClassLoader classLoader = plugin.getClass().getClassLoader(); // NOPMD - Load the supplying plugin's API.
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
