package net.enthusia.autoclicker.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

final class AutoClickService {
    private final EnthusiaServerAutoClickerPlugin plugin;
    private final CombatXHook combatX;
    private final Map<UUID, AutoClickSession> sessions = new HashMap<>();
    private final Set<UUID> attackingPlayers = new HashSet<>();

    AutoClickService(EnthusiaServerAutoClickerPlugin plugin, CombatXHook combatX) {
        this.plugin = plugin;
        this.combatX = combatX;
    }

    void enableCooldown(Player player) {
        sessions.put(player.getUniqueId(), new AutoClickSession(
            player.getUniqueId(),
            player.getLocation(),
            AutoClickMode.COOLDOWN,
            0
        ));
        player.sendMessage("AutoClicker enabled: cooldown mode.");
    }

    void enableFixed(Player player, int intervalTicks) {
        sessions.put(player.getUniqueId(), new AutoClickSession(
            player.getUniqueId(),
            player.getLocation(),
            AutoClickMode.FIXED_INTERVAL,
            intervalTicks
        ));
        player.sendMessage("AutoClicker enabled: every " + intervalTicks + " ticks.");
    }

    void disable(Player player, String reason) {
        AutoClickSession removed = sessions.remove(player.getUniqueId());
        if (removed != null && reason != null && !reason.isBlank()) {
            player.sendMessage("AutoClicker stopped: " + reason);
        }
    }

    boolean isEnabled(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    boolean isAutoAttacking(Player player) {
        return attackingPlayers.contains(player.getUniqueId());
    }

    String status(Player player) {
        AutoClickSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return "AutoClicker is off.";
        }
        if (session.mode() == AutoClickMode.COOLDOWN) {
            return "AutoClicker is on: cooldown mode.";
        }
        return "AutoClicker is on: every " + session.intervalTicks() + " ticks.";
    }

    void tick() {
        for (UUID playerId : Set.copyOf(sessions.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            AutoClickSession session = sessions.get(playerId);
            if (player == null || session == null) {
                sessions.remove(playerId);
                continue;
            }
            tick(player, session);
        }
    }

    private void tick(Player player, AutoClickSession session) {
        if (!combatX.isAvailable()) {
            disable(player, "CombatX is unavailable");
            return;
        }
        if (combatX.isInCombat(player)) {
            disable(player, "you are in combat");
            return;
        }
        if (!canUse(player)) {
            disable(player, "you cannot attack right now");
            return;
        }
        if (movedTooFar(player, session)) {
            disable(player, "you moved too far");
            return;
        }
        boolean ready = session.mode() == AutoClickMode.COOLDOWN
            ? player.getAttackCooldown() >= 1.0F
            : session.consumeFixedIntervalTick();
        if (!ready) {
            return;
        }
        LivingEntity target = findTarget(player);
        if (target == null) {
            if (plugin.stopWhenNoTarget()) {
                disable(player, "no valid target");
            }
            return;
        }
        attack(player, target);
    }

    private boolean canUse(Player player) {
        return player.isOnline()
            && !player.isDead()
            && player.isValid()
            && player.getGameMode() != GameMode.SPECTATOR
            && player.getGameMode() != GameMode.CREATIVE;
    }

    private boolean movedTooFar(Player player, AutoClickSession session) {
        Location anchor = session.anchor();
        Location current = player.getLocation();
        if (!sameWorld(anchor.getWorld(), current.getWorld())) {
            return true;
        }
        double max = plugin.maxMovementBlocks();
        double dx = current.getX() - anchor.getX();
        double dz = current.getZ() - anchor.getZ();
        double dy = Math.max(0.0D, Math.abs(current.getY() - anchor.getY()) - 0.5D);
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    private boolean sameWorld(World first, World second) {
        return first != null && second != null && first.getUID().equals(second.getUID());
    }

    private LivingEntity findTarget(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(
            eye,
            direction,
            plugin.attackRangeBlocks(),
            plugin.raySizeBlocks(),
            entity -> isValidTarget(player, entity)
        );
        if (result == null || !(result.getHitEntity() instanceof LivingEntity livingEntity)) {
            return null;
        }
        return livingEntity;
    }

    private boolean isValidTarget(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity) || entity instanceof Player) {
            return false;
        }
        return !entity.getUniqueId().equals(player.getUniqueId())
            && entity.isValid()
            && !livingEntity.isDead()
            && player.hasLineOfSight(entity);
    }

    private void attack(Player player, LivingEntity target) {
        attackingPlayers.add(player.getUniqueId());
        try {
            player.swingMainHand();
            player.attack(target);
        } finally {
            attackingPlayers.remove(player.getUniqueId());
        }
    }

    void disableAll(String reason) {
        for (UUID playerId : Set.copyOf(sessions.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                disable(player, reason);
            } else {
                sessions.remove(playerId);
            }
        }
    }
}
