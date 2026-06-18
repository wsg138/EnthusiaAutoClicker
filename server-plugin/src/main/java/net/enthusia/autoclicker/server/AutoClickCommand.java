package net.enthusia.autoclicker.server;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class AutoClickCommand implements CommandExecutor, TabCompleter {
    private final EnthusiaServerAutoClickerPlugin plugin;
    private final AutoClickService service;

    AutoClickCommand(EnthusiaServerAutoClickerPlugin plugin, AutoClickService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            service.enableCooldown(player);
            return true;
        }
        String argument = args[0].toLowerCase();
        if (argument.equals("off") || argument.equals("stop") || argument.equals("disable")) {
            service.disable(player, "disabled");
            return true;
        }
        if (argument.equals("status")) {
            player.sendMessage(service.status(player));
            return true;
        }
        try {
            int intervalTicks = Integer.parseInt(argument);
            if (intervalTicks < plugin.minimumFixedIntervalTicks()) {
                player.sendMessage("Interval must be at least " + plugin.minimumFixedIntervalTicks() + " ticks.");
                return true;
            }
            service.enableFixed(player, intervalTicks);
            return true;
        } catch (NumberFormatException exception) {
            player.sendMessage("Usage: /autoclick [ticks|off|status]");
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(List.of("off", "status", "20"));
        options.removeIf(option -> !option.startsWith(args[0].toLowerCase()));
        return options;
    }
}
