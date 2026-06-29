package net.enthusia.autoclicker.server;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnthusiaServerAutoClickerPlugin extends JavaPlugin {
    private CombatXHook combatX;
    private AutoClickService service;
    private ClientHandshakeService handshakeService;
    private double maxMovementBlocks;
    private double attackRangeBlocks;
    private double raySizeBlocks;
    private int minimumFixedIntervalTicks;
    private boolean stopWhenNoTarget;
    private boolean stopOnTeleport;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        combatX = new CombatXHook();
        if (!combatX.initialize(this)) {
            getLogger().warning("CombatX is required. The plugin will load, but /autoclick will stay blocked.");
        }

        service = new AutoClickService(this, combatX);
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

    boolean stopWhenNoTarget() {
        return stopWhenNoTarget;
    }

    boolean stopOnTeleport() {
        return stopOnTeleport;
    }
}
