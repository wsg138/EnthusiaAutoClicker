package net.enthusia.autoclicker.server;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        if (hasAction(args, "check")) {
            return checkPlayer(sender, args);
        }
        if (hasAction(args, "reload")) {
            return reload(sender);
        }
        return handlePlayerCommand(sender, args);
    }

    private boolean handlePlayerCommand(CommandSender sender, String[] args) {
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
        return handlePlayerAction(player, args[0].toLowerCase(Locale.ROOT));
    }

    private boolean handlePlayerAction(Player player, String argument) {
        return switch (argument) {
            case "off", "stop", "disable" -> stop(player);
            case "status" -> showStatus(player);
            default -> enableFixed(player, argument);
        };
    }

    private boolean stop(Player player) {
        service.disable(player, "disabled");
        return true;
    }

    private boolean showStatus(Player player) {
        player.sendMessage(service.status(player));
        return true;
    }

    private boolean enableFixed(Player player, String argument) {
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

    private static boolean hasAction(String[] args, String action) {
        return args.length > 0 && args[0].equalsIgnoreCase(action);
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("enthusia.autoclicker.admin")) {
            sender.sendMessage(error("You do not have permission to reload this plugin."));
            return true;
        }
        plugin.reloadSettingsForCommand();
        service.disableAll("configuration reloaded");
        sender.sendMessage(ChatColor.GREEN + "Enthusia AutoClicker configuration reloaded. Active sessions were stopped.");
        return true;
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
            sender.sendMessage(ChatColor.GRAY + "This is a convenience signal, not secure proof of the exact client mod.");
            return true;
        }
        ClientHandshake detected = handshake.get();
        sender.sendMessage(ChatColor.GOLD + "Enthusia AutoClicker was " + ChatColor.GREEN
            + "DETECTED" + ChatColor.GOLD + " for " + ChatColor.WHITE + target.getName()
            + ChatColor.GOLD + ".");
        sender.sendMessage(ChatColor.GRAY + "Mod version: " + ChatColor.WHITE + detected.modVersion());
        sender.sendMessage(ChatColor.GRAY + "Loader: " + ChatColor.WHITE + detected.loader());
        sender.sendMessage(ChatColor.GRAY + "Minecraft: " + ChatColor.WHITE + detected.minecraftVersion());
        sender.sendMessage(ChatColor.GRAY + "Received: " + ChatColor.WHITE
            + DateTimeFormatter.ISO_INSTANT.format(detected.receivedAt()));
        sender.sendMessage(ChatColor.GRAY + "This is a convenience signal, not secure proof of the exact client mod.");
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
            List<String> options = new ArrayList<>(List.of("off", "status", "check", "reload", "20"));
            String prefix = args[0].toLowerCase(Locale.ROOT);
            options.removeIf(option -> !option.startsWith(prefix));
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("check")
            && sender.hasPermission("enthusia.autoclicker.check")) {
            List<String> names = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                names.add(player.getName());
            }
            String prefix = args[1].toLowerCase(Locale.ROOT);
            names.removeIf(name -> !name.toLowerCase(Locale.ROOT).startsWith(prefix));
            return names;
        }
        return List.of();
    }
}
