package net.enthusia.autoclicker.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.ChatColor;
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
    private final ClientHandshakeService handshakeService;

    AutoClickCommand(
        EnthusiaServerAutoClickerPlugin plugin,
        AutoClickService service,
        ClientHandshakeService handshakeService
    ) {
        this.plugin = plugin;
        this.service = service;
        this.handshakeService = handshakeService;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length > 0 && args[0].equalsIgnoreCase("check")) {
            return checkPlayer(sender, args);
        }
        if (!sender.hasPermission("enthusia.autoclicker.use")) {
            sender.sendMessage(error("You do not have permission to use this command."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can use this command."));
            return true;
        }
        if (args.length == 0) {
            if (service.isEnabled(player)) {
                service.disable(player, "disabled");
                return true;
            }
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
                player.sendMessage(error("Interval must be at least " + plugin.minimumFixedIntervalTicks() + " ticks."));
                return true;
            }
            service.enableFixed(player, intervalTicks);
            return true;
        } catch (NumberFormatException exception) {
            player.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/autoclick [ticks|off|status]");
            return true;
        }
    }

    private boolean checkPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("enthusia.autoclicker.check")) {
            sender.sendMessage(error("You do not have permission to use this command."));
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/autoclick check <player>");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(error("Player not found: " + args[1]));
            return true;
        }

        Optional<ClientHandshake> handshake = handshakeService.handshake(target);
        if (handshake.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "Enthusia AutoClicker was " + ChatColor.RED
                + "NOT DETECTED" + ChatColor.GOLD + " for " + ChatColor.WHITE + target.getName()
                + ChatColor.GOLD + ".");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Enthusia AutoClicker was " + ChatColor.GREEN
            + "DETECTED" + ChatColor.GOLD + " for " + ChatColor.WHITE + target.getName()
            + ChatColor.GOLD + ".");
        return true;
    }

    private String error(String message) {
        return ChatColor.RED + message;
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("off", "status", "check", "20"));
            options.removeIf(option -> !option.startsWith(args[0].toLowerCase()));
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("check")
            && sender.hasPermission("enthusia.autoclicker.check")) {
            List<String> names = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                names.add(player.getName());
            }
            names.removeIf(name -> !name.toLowerCase().startsWith(args[1].toLowerCase()));
            return names;
        }
        if (args.length != 1) {
            return List.of();
        }
        return List.of();
    }
}
