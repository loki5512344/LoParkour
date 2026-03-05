package dev.loki.loparkour.command;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all player-facing /parkour sub-commands.
 * Admin commands are in {@link AdminCommandHandler}.
 */
public class PlayerCommandHandler {

    private final Map<String, Long> cooldowns = new HashMap<>();

    public void handleNoArgs(@NotNull CommandSender sender, @Nullable Player player) {
        if (player != null && ParkourOption.MAIN.mayPerform(player)) {
            Menus.MAIN.open(player);
        } else {
            sendHelp(sender);
        }
    }

    public void handle(@NotNull String arg, @NotNull CommandSender sender, @Nullable Player player) {
        switch (arg.toLowerCase()) {
            case "help"   -> sendHelp(sender);
            case "reload" -> handleReload(sender, player);
        }

        if (player == null) return;

        switch (arg.toLowerCase()) {
            case "join" -> {
                if (!cooldown(sender, "join", 2500)) return;
                if (!ParkourOption.JOIN.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }
                if (ParkourUser.getUser(player) != null) return;
                Modes.DEFAULT.create(player);
            }
            case "leave" -> {
                if (!cooldown(sender, "leave", 2500)) return;
                ParkourUser.leave(player);
            }
            case "play" -> {
                if (ParkourOption.PLAY.mayPerform(player)) Menus.PLAY.open(player);
            }
            case "menu", "main" -> {
                if (ParkourOption.MAIN.mayPerform(player)) Menus.MAIN.open(player);
            }
            case "leaderboard" -> player.performCommand("LoParkour leaderboard invalid");
            case "schematic" -> {
                if (!player.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }
                SchematicCommandHandler.sendHelp(player);
            }
        }
    }

    private void handleReload(@NotNull CommandSender sender, @Nullable Player player) {
        if (!cooldown(sender, "reload", 2500)) return;
        if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, Locales.getString(player, "other.no_do"));
            return;
        }
        Config.reload(false);
        send(sender, LoParkour.PREFIX + "Reloaded config files.");
    }

    public void sendHelp(@NotNull CommandSender sender) {
        send(sender, "");
        send(sender, "<dark_gray><strikethrough>---------------<reset> " + LoParkour.NAME + " <dark_gray><strikethrough>---------------<reset>");
        send(sender, "");
        send(sender, "<gray>/parkour <dark_gray>- Main command");
        if (sender.hasPermission(ParkourOption.JOIN.permission)) {
            send(sender, "<gray>/parkour join [mode/player] <dark_gray>- Join a mode");
            send(sender, "<gray>/parkour leave <dark_gray>- Leave parkour");
        }
        if (sender.hasPermission(ParkourOption.MAIN.permission))
            send(sender, "<gray>/parkour menu <dark_gray>- Open the menu");
        if (sender.hasPermission(ParkourOption.PLAY.permission))
            send(sender, "<gray>/parkour play <dark_gray>- Mode selection menu");
        if (sender.hasPermission(ParkourOption.LEADERBOARDS.permission))
            send(sender, "<gray>/parkour leaderboard [type] <dark_gray>- Leaderboard");
        if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, "<gray>/LoParkour schematic <dark_gray>- Schematic tools");
            send(sender, "<gray>/LoParkour reload <dark_gray>- Reload configs");
            send(sender, "<gray>/LoParkour reset <everyone/player> <dark_gray>- Reset scores");
            send(sender, "<gray>/LoParkour forcejoin/forceleave <target> <dark_gray>- Force join/leave");
            send(sender, "<gray>/LoParkour recoverinventory <player> <dark_gray>- Recover inventory");
        }
        send(sender, "");
    }

    public boolean cooldown(CommandSender sender, String key, long millis) {
        String fullKey = sender.getName() + ":" + key;
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(fullKey);
        if (last != null && now - last < millis) return false;
        cooldowns.put(fullKey, now);
        return true;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.color(message));
    }
}
