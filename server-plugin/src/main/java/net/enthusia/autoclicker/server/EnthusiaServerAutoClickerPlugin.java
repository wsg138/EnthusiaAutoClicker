package net.enthusia.autoclicker.server;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class EnthusiaServerAutoClickerPlugin extends JavaPlugin {
    private CombatXHook combatX;
    private AutoClickService service;
    private InternalCombatTracker internalCombatTracker;
    private ClientHandshakeService handshakeService;
    private double maxMovementBlocks;
    private double attackRangeBlocks;
    private double raySizeBlocks;
    private int minimumFixedIntervalTicks;
    private int internalCombatDurationTicks;
    private boolean requireCombatX;
    private boolean requireLineOfSight;
    private boolean preventAttackingThroughBlocks;
    private boolean allowThroughPassableBlocks;
    private boolean stopWhenInventoryOpen;
    private boolean swingWhenNoTarget;
    private boolean stopWhenNoTarget;
    private boolean stopOnTeleport;
    private TargetFilterMode targetFilterMode;
    private Set<EntityType> allowedTargetTypes;
    private Set<EntityType> deniedTargetTypes;
    private boolean denyTamedAnimals;
    private boolean denyPassiveAnimals;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        combatX = new CombatXHook();
        combatX.initialize(this);

        internalCombatTracker = new InternalCombatTracker(this);
        service = new AutoClickService(this, combatX, internalCombatTracker);
        handshakeService = new ClientHandshakeService();
        getServer().getMessenger().registerIncomingPluginChannel(
            this,
            ClientHandshakeService.CHANNEL,
            handshakeService
        );
        AutoClickCommand commandExecutor = new AutoClickCommand(this, service, handshakeService);
        PluginCommand command = getCommand("autoclick");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new AutoClickListener(this, service, handshakeService), this);
        getServer().getPluginManager().registerEvents(internalCombatTracker, this);
        getServer().getScheduler().runTaskTimer(this, service::tick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.disableAll("plugin disabled");
        }
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    private void reloadSettings() {
        maxMovementBlocks = Math.max(0.0D, getConfig().getDouble("max-movement-blocks", 0.75D));
        attackRangeBlocks = Math.max(0.1D, getConfig().getDouble("attack-range-blocks", 3.0D));
        raySizeBlocks = Math.max(0.35D, getConfig().getDouble("ray-size-blocks", 0.35D));
        minimumFixedIntervalTicks = Math.max(1, getConfig().getInt("minimum-fixed-interval-ticks", 1));
        internalCombatDurationTicks = Math.max(1, getConfig().getInt("internal-combat-duration-ticks", 200));
        requireCombatX = getConfig().getBoolean("require-combatx", false);
        requireLineOfSight = getConfig().getBoolean("require-line-of-sight", false);
        preventAttackingThroughBlocks = getConfig().getBoolean("prevent-attacking-through-blocks", true);
        allowThroughPassableBlocks = getConfig().getBoolean("allow-through-passable-blocks", true);
        stopWhenInventoryOpen = getConfig().getBoolean("stop-when-inventory-open", true);
        swingWhenNoTarget = getConfig().getBoolean("swing-when-no-target", true);
        stopWhenNoTarget = getConfig().getBoolean("stop-when-no-target", false);
        stopOnTeleport = getConfig().getBoolean("stop-on-teleport", true);
        targetFilterMode = TargetFilterMode.from(getConfig().getString("target-filter.mode", "DENYLIST"));
        allowedTargetTypes = parseEntityTypes("target-filter.allowed-types");
        deniedTargetTypes = parseEntityTypes("target-filter.denied-types");
        denyTamedAnimals = getConfig().getBoolean("target-filter.deny-tamed-animals", true);
        denyPassiveAnimals = getConfig().getBoolean("target-filter.deny-passive-animals", true);
    }

    void reloadSettingsForCommand() {
        reloadConfig();
        reloadSettings();
        if (combatX != null) {
            combatX.resetAvailabilityWarning();
            combatX.refresh();
        }
    }

    private Set<EntityType> parseEntityTypes(String path) {
        Set<EntityType> types = EnumSet.noneOf(EntityType.class);
        for (String configured : getConfig().getStringList(path)) {
            try {
                types.add(EntityType.valueOf(configured.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Ignoring unknown entity type in " + path + ": " + configured);
            }
        }
        return types;
    }

    double maxMovementBlocks() {
        return maxMovementBlocks;
    }

    double attackRangeBlocks() {
        return attackRangeBlocks;
    }

    double raySizeBlocks() {
        return raySizeBlocks;
    }

    int minimumFixedIntervalTicks() {
        return minimumFixedIntervalTicks;
    }

    int internalCombatDurationTicks() {
        return internalCombatDurationTicks;
    }

    boolean requireCombatX() {
        return requireCombatX;
    }

    boolean requireLineOfSight() {
        return requireLineOfSight;
    }

    boolean preventAttackingThroughBlocks() {
        return preventAttackingThroughBlocks;
    }

    boolean allowThroughPassableBlocks() {
        return allowThroughPassableBlocks;
    }

    boolean stopWhenInventoryOpen() {
        return stopWhenInventoryOpen;
    }

    boolean swingWhenNoTarget() {
        return swingWhenNoTarget;
    }

    boolean stopWhenNoTarget() {
        return stopWhenNoTarget;
    }

    boolean stopOnTeleport() {
        return stopOnTeleport;
    }

    TargetFilterMode targetFilterMode() {
        return targetFilterMode;
    }

    Set<EntityType> allowedTargetTypes() {
        return allowedTargetTypes;
    }

    Set<EntityType> deniedTargetTypes() {
        return deniedTargetTypes;
    }

    boolean denyTamedAnimals() {
        return denyTamedAnimals;
    }

    boolean denyPassiveAnimals() {
        return denyPassiveAnimals;
    }

    enum TargetFilterMode {
        ALLOWLIST,
        DENYLIST;

        static TargetFilterMode from(String value) {
            if (value != null && value.equalsIgnoreCase("ALLOWLIST")) {
                return ALLOWLIST;
            }
            return DENYLIST;
        }
    }
}
