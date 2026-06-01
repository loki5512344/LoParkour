package dev.loki.loparkour.command.player;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.command.schematic.SchematicCommandHandler;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.loki.loparkour.util.text.ColorUtil;
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
            case "leaderboard" -> {
                if (!ParkourOption.LEADERBOARDS.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }
                Menus.LEADERBOARDS.open(player);
            }
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
            send(sender, Locales.getString(sender, "other.no_do"));
            return;
        }
        Config.reload(false);
        dev.loki.loparkour.adaptive.bootstrap.AdaptiveServices.reload();
        send(sender, LoParkour.PREFIX + Locales.getString(sender, "commands.reload"));
    }

    public void sendHelp(@NotNull CommandSender sender) {
        send(sender, "");
        send(sender, Locales.getString(sender, "commands.help.header"));
        send(sender, "");
        send(sender, Locales.getString(sender, "commands.help.line_parkour"));
        if (sender.hasPermission(ParkourOption.JOIN.permission)) {
            send(sender, Locales.getString(sender, "commands.help.line_join"));
            send(sender, Locales.getString(sender, "commands.help.line_leave"));
        }
        if (sender.hasPermission(ParkourOption.MAIN.permission)) {
            send(sender, Locales.getString(sender, "commands.help.line_menu"));
        }
        if (sender.hasPermission(ParkourOption.PLAY.permission)) {
            send(sender, Locales.getString(sender, "commands.help.line_play"));
        }
        if (sender.hasPermission(ParkourOption.LEADERBOARDS.permission)) {
            send(sender, Locales.getString(sender, "commands.help.line_leaderboard"));
        }
        if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, Locales.getString(sender, "commands.help.line_schematic"));
            send(sender, Locales.getString(sender, "commands.help.line_reload"));
            send(sender, Locales.getString(sender, "commands.help.line_reset"));
            send(sender, Locales.getString(sender, "commands.help.line_force"));
            send(sender, Locales.getString(sender, "commands.help.line_recover"));
        }
        send(sender, "");
    }

    public boolean cooldown(CommandSender sender, String key, long millis) {
        return cooldown(sender, key, millis, null);
    }

    /**
     * @param whenBlocked message sent ({@code &#RRGGBB} / {@code &} colors) if still on cooldown; may be null for silent block
     */
    public boolean cooldown(CommandSender sender, String key, long millis, @Nullable String whenBlocked) {
        String fullKey = sender.getName() + ":" + key;
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(fullKey);
        if (last != null && now - last < millis) {
            if (whenBlocked != null) {
                send(sender, whenBlocked);
            }
            return false;
        }
        cooldowns.put(fullKey, now);
        return true;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.color(message));
    }
}
