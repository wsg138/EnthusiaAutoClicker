package net.enthusia.autoclicker.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

final class InternalCombatTracker implements Listener {
    private final EnthusiaServerAutoClickerPlugin plugin;
    private final Map<UUID, Long> combatUntilTicks = new HashMap<>();

    InternalCombatTracker(EnthusiaServerAutoClickerPlugin plugin) {
        this.plugin = plugin;
    }

    boolean isInCombat(Player player) {
        Long combatUntilTick = combatUntilTicks.get(player.getUniqueId());
        if (combatUntilTick == null) {
            return false;
        }
        if (plugin.getServer().getCurrentTick() > combatUntilTick) {
            combatUntilTicks.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    void cleanupExpired() {
        long currentTick = plugin.getServer().getCurrentTick();
        Iterator<Map.Entry<UUID, Long>> iterator = combatUntilTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < currentTick) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCombat(EntityDamageByEntityEvent event) {
        Player damaged = asPlayer(event.getEntity());
        Player damager = damager(event.getDamager());
        if (damaged == null || damager == null || damaged.getUniqueId().equals(damager.getUniqueId())) {
            return;
        }
        long combatUntilTick = plugin.getServer().getCurrentTick() + plugin.internalCombatDurationTicks();
        combatUntilTicks.put(damaged.getUniqueId(), combatUntilTick);
        combatUntilTicks.put(damager.getUniqueId(), combatUntilTick);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatUntilTicks.remove(event.getPlayer().getUniqueId());
    }

    private Player asPlayer(Entity entity) {
        return entity instanceof Player player ? player : null;
    }

    private Player damager(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
