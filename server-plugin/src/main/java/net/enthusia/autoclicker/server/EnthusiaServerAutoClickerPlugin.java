package net.enthusia.autoclicker.server;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

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
    private boolean stopWhenNoTarget;
    private boolean stopOnTeleport;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        combatX = new CombatXHook();
        if (!combatX.initialize(this)) {
            getLogger().warning("CombatX could not be hooked. Falling back to the plugin's internal PvP combat tracker.");
        }

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
        raySizeBlocks = Math.max(0.0D, getConfig().getDouble("ray-size-blocks", 0.15D));
        minimumFixedIntervalTicks = Math.max(1, getConfig().getInt("minimum-fixed-interval-ticks", 1));
        internalCombatDurationTicks = Math.max(1, getConfig().getInt("internal-combat-duration-ticks", 200));
        requireCombatX = getConfig().getBoolean("require-combatx", false);
        stopWhenNoTarget = getConfig().getBoolean("stop-when-no-target", false);
        stopOnTeleport = getConfig().getBoolean("stop-on-teleport", true);
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

    boolean stopWhenNoTarget() {
        return stopWhenNoTarget;
    }

    boolean stopOnTeleport() {
        return stopOnTeleport;
    }
}
