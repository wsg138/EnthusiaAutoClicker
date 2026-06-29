package net.enthusia.autoclicker.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
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

    private boolean checkPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("enthusia.autoclicker.check")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage("Usage: /autoclick check <player>");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[1]);
            return true;
        }

        Optional<ClientHandshake> handshake = handshakeService.handshake(target);
        if (handshake.isEmpty()) {
            sender.sendMessage("AutoClicker mod check for " + target.getName() + ": NOT DETECTED");
            sender.sendMessage("No private handshake has been received for this login session.");
            return true;
        }
        ClientHandshake detected = handshake.get();
        sender.sendMessage("AutoClicker mod check for " + target.getName() + ": DETECTED");
        sender.sendMessage("Mod version: " + detected.modVersion());
        sender.sendMessage("Loader: " + detected.loader());
        sender.sendMessage("Minecraft: " + detected.minecraftVersion());
        sender.sendMessage("Received: " + detected.receivedAt());
        return true;
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
