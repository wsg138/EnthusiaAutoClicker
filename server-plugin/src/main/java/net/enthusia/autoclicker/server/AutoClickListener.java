package net.enthusia.autoclicker.server;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

final class AutoClickListener implements Listener {
    private final EnthusiaServerAutoClickerPlugin plugin;
    private final AutoClickService service;

    AutoClickListener(EnthusiaServerAutoClickerPlugin plugin, AutoClickService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAutoAttackDamagesPlayer(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        if (!service.isAutoAttacking(player)) {
            return;
        }
        event.setCancelled(true);
        service.disable(player, "auto-attack would damage a player");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.disable(event.getPlayer(), "");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        service.disable(event.getEntity(), "you died");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        service.disable(event.getPlayer(), "world changed");
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.stopOnTeleport()) {
            service.disable(event.getPlayer(), "teleported");
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (service.isEnabled(event.getPlayer())) {
            service.disable(event.getPlayer(), "game mode changed");
        }
    }
}
