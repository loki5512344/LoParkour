package dev.loki.loparkour;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.command.AdminCommandHandler;
import dev.loki.loparkour.command.PlayerCommandHandler;
import dev.loki.loparkour.command.SchematicCommandHandler;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.mode.MultiMode;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Main command router for /LoParkour and /parkour.
 * Delegates to {@link PlayerCommandHandler}, {@link AdminCommandHandler},
 * and {@link SchematicCommandHandler}.
 */
public class LoParkourCommand implements CommandExecutor, TabCompleter {

    private final PlayerCommandHandler player = new PlayerCommandHandler();
    private final AdminCommandHandler admin = new AdminCommandHandler(player);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        try {
            Player p = sender instanceof Player ? (Player) sender : null;

            switch (args.length) {
                case 0 -> player.handleNoArgs(sender, p);
                case 1 -> player.handle(args[0], sender, p);
                case 2 -> handle2(args[0], args[1], sender, p);
                case 3 -> handle3(args[0], args[1], args[2], sender, p);
                default -> sender.sendMessage(ChatColor.GRAY + "Too many arguments. Try /parkour help");
            }
            return true;
        } catch (Throwable t) {
            LoParkour.getPlugin().getLogger().log(Level.SEVERE,
                    "LoParkour command failed (" + label + " " + String.join(" ", args) + ")", t);
            sender.sendMessage(ChatColor.RED + "LoParkour: command error — see server console.");
            return true;
        }
    }

    // ── 2-arg routing ──────────────────────────────────────────────────────────

    private void handle2(String a1, String a2, CommandSender sender, @Nullable Player p) {
        // Admin commands that don't need a player
        switch (a1.toLowerCase()) {
            case "forcejoin", "forceleave", "reset", "recoverinventory" -> {
                admin.handle(a1, a2, sender, p);
                return;
            }
        }

        if (p == null) return;

        switch (a1.toLowerCase()) {
            case "join" -> handleJoin(a2, sender, p);
            case "leaderboard" -> handleLeaderboard(a2, sender, p);
            case "schematic" -> {
                if (!p.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(p, "other.no_do"));
                    return;
                }
                SchematicCommandHandler.handleSubcommand(a2, sender, p, player);
            }
        }
    }

    // ── 3-arg routing ──────────────────────────────────────────────────────────

    private void handle3(String a1, String a2, String a3, CommandSender sender, @Nullable Player p) {
        if (p == null) return;
        if (!a1.equalsIgnoreCase("schematic")) return;
        if (!p.hasPermission(ParkourOption.ADMIN.permission)) return;
        SchematicCommandHandler.handleSubcommandWithName(a2, a3, sender, p, player);
    }

    // ── join logic ─────────────────────────────────────────────────────────────

    private void handleJoin(String arg, CommandSender sender, Player p) {
        if (!player.cooldown(sender, "join", 2500) || !ParkourOption.JOIN.mayPerform(p)) return;

        Mode mode = Registry.getMode(arg);
        if (mode != null) { mode.create(p); return; }

        Player other = Bukkit.getPlayer(arg);
        if (other == null) { send(sender, LoParkour.PREFIX + "Unknown player."); return; }

        ParkourPlayer pp = ParkourPlayer.getPlayer(other);
        if (pp == null) { send(sender, LoParkour.PREFIX + "That player isn't playing."); return; }

        ParkourUser user = ParkourUser.getUser(p);
        Session session = pp.session;
        if (user != null && user.session == session) return;

        if (session.isAcceptingPlayers()) {
            Mode sessionMode = session.generator.getMode();
            if (sessionMode instanceof MultiMode mm) {
                mm.join(p, session);
            } else {
                Modes.SPECTATOR.create(p, session);
            }
        } else {
            Modes.SPECTATOR.create(p, session);
        }
    }

    // ── leaderboard logic ──────────────────────────────────────────────────────

    private void handleLeaderboard(String arg, CommandSender sender, Player p) {
        if (!ParkourOption.LEADERBOARDS.mayPerform(p)) {
            send(sender, Locales.getString(p, "other.no_do"));
            return;
        }
        Mode mode = Registry.getMode(arg.toLowerCase());
        if (mode == null) {
            Menus.LEADERBOARDS.open(p);
        } else {
            Menus.SINGLE_LEADERBOARD.open(p, mode, Leaderboard.Sort.SCORE);
        }
    }

    // ── tab complete ───────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (ParkourOption.JOIN.mayPerform(sender))   { completions.add("join"); completions.add("leave"); }
            if (ParkourOption.MAIN.mayPerform(sender))    completions.add("menu");
            if (ParkourOption.PLAY.mayPerform(sender))    completions.add("play");
            if (ParkourOption.LEADERBOARDS.mayPerform(sender)) completions.add("leaderboard");
            if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.addAll(List.of("schematic", "reload", "forcejoin", "forceleave", "reset", "recoverinventory"));
            }
            return filter(args[0], completions);
        }

        if (args.length == 2) {
            String a1 = args[0].toLowerCase();
            if (a1.equals("reset") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.add("everyone");
                ParkourPlayer.getPlayers().forEach(pp -> completions.add(pp.getName()));
            } else if (a1.equals("join") && ParkourOption.JOIN.mayPerform(sender)) {
                Registry.getModes().forEach(m -> completions.add(m.getName()));
                ParkourPlayer.getPlayers().forEach(pp -> completions.add(pp.getName()));
            } else if (a1.equals("leaderboard") && ParkourOption.LEADERBOARDS.mayPerform(sender)) {
                Registry.getModes().forEach(m -> completions.add(m.getName()));
            } else if (a1.equals("schematic") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.addAll(Arrays.asList("wand", "pos1", "pos2", "save", "paste", "list", "reload"));
            } else if ((a1.equals("forcejoin") || a1.equals("forceleave")) && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.add("everyone");
                if (a1.equals("forcejoin")) completions.add("nearest");
                Bukkit.getOnlinePlayers().forEach(pl -> completions.add(pl.getName()));
            } else if (a1.equals("recoverinventory") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                Bukkit.getOnlinePlayers().forEach(pl -> completions.add(pl.getName()));
            }
            return filter(args[1], completions);
        }

        return Collections.emptyList();
    }

    // ── util ───────────────────────────────────────────────────────────────────

    private List<String> filter(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(ColorUtil.color(msg));
    }
}
