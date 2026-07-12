package dev.loki.loparkour.command;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.command.admin.AdminCommandHandler;
import dev.loki.loparkour.command.player.PlayerCommandHandler;
import dev.loki.loparkour.command.schematic.SchematicCommandHandler;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.mode.base.MultiMode;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.loki.loparkour.schematic.core.SchematicManager;
import dev.loki.loparkour.session.core.Session;
import dev.loki.loparkour.util.text.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Main command router for /LoParkour and /parkour.
 * Delegates to {@link PlayerCommandHandler}, {@link AdminCommandHandler},
 * and {@link SchematicCommandHandler}.
 */
public class LoParkourCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SCHEMATIC_DIFFICULTIES = List.of("0.0", "0.25", "0.5", "0.75", "1.0");
    private static final List<String> SCHEMATIC_SUBCOMMANDS = List.of(
            "wand", "pos1", "pos2", "create", "convert", "paste", "list", "reload"
    );

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
                case 4 -> handle4(args[0], args[1], args[2], args[3], sender, p);
                default -> Locales.send(sender, "commands.too_many_args");
            }
            return true;
        } catch (Throwable t) {
            LoParkour.getPlugin().getLogger().log(Level.SEVERE,
                    "LoParkour command failed (" + label + " " + String.join(" ", args) + ")", t);
            Locales.send(sender, "commands.error");
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
            default -> {
            }
        }

        if (p == null) {
            return;
        }

        if ("create".equalsIgnoreCase(a1) && p.hasPermission(ParkourOption.ADMIN.permission)) {
            SchematicCommandHandler.handleCreateDifficultyOnly(a2, sender, p, player);
            return;
        }

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
            default -> {
            }
        }
    }

    // ── 3-arg routing ──────────────────────────────────────────────────────────

    private void handle3(String a1, String a2, String a3, CommandSender sender, @Nullable Player p) {
        if (p == null) {
            return;
        }
        if (!p.hasPermission(ParkourOption.ADMIN.permission)) {
            return;
        }

        if ("schematic".equalsIgnoreCase(a1)) {
            if ("create".equalsIgnoreCase(a2)) {
                SchematicCommandHandler.handleCreateDifficultyOnly(a3, sender, p, player);
                return;
            }
            SchematicCommandHandler.handleSubcommandWithName(a2, a3, sender, p);
        }
    }

    private void handle4(String a1, String a2, String a3, String a4, CommandSender sender, @Nullable Player p) {
        if (p == null) {
            return;
        }
        if (!p.hasPermission(ParkourOption.ADMIN.permission)) {
            return;
        }
        if (!"schematic".equalsIgnoreCase(a1) || !"create".equalsIgnoreCase(a2)) {
            return;
        }
        SchematicCommandHandler.handleCreateWithDifficulty(a3, a4, sender, p, player);
    }

    // ── join logic ─────────────────────────────────────────────────────────────

    private void handleJoin(String arg, CommandSender sender, Player p) {
        if (!player.cooldown(sender, "join", 2500) || !ParkourOption.JOIN.mayPerform(p)) {
            return;
        }

        Mode mode = Registry.getMode(arg);
        if (mode != null) {
            mode.create(p);
            return;
        }

        Player other = Bukkit.getPlayer(arg);
        if (other == null) {
            send(sender, LoParkour.PREFIX + Locales.getString(sender, "commands.unknown_player"));
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(other);
        if (pp == null) {
            send(sender, LoParkour.PREFIX + Locales.getString(sender, "commands.not_playing"));
            return;
        }

        ParkourUser user = ParkourUser.getUser(p);
        Session session = pp.session;
        if (user != null && user.session == session) {
            return;
        }

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
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (ParkourOption.JOIN.mayPerform(sender)) {
                completions.add("join");
                completions.add("leave");
            }
            if (ParkourOption.MAIN.mayPerform(sender)) {
                completions.add("menu");
            }
            if (ParkourOption.PLAY.mayPerform(sender)) {
                completions.add("play");
            }
            if (ParkourOption.LEADERBOARDS.mayPerform(sender)) {
                completions.add("leaderboard");
            }
            if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.addAll(List.of("schematic", "create", "reload", "forcejoin", "forceleave", "reset", "recoverinventory"));
            }
            return filter(args[0], completions);
        }

        if (args.length == 2) {
            String a1 = args[0].toLowerCase();
            if ("reset".equals(a1) && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.add("everyone");
                ParkourPlayer.getPlayers().forEach(pp -> completions.add(pp.getName()));
            } else if ("join".equals(a1) && ParkourOption.JOIN.mayPerform(sender)) {
                Registry.getModes().forEach(m -> completions.add(m.getName()));
                ParkourPlayer.getPlayers().forEach(pp -> completions.add(pp.getName()));
            } else if ("leaderboard".equals(a1) && ParkourOption.LEADERBOARDS.mayPerform(sender)) {
                Registry.getModes().forEach(m -> completions.add(m.getName()));
            } else if ("schematic".equals(a1) && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.addAll(SCHEMATIC_SUBCOMMANDS);
            } else if ("create".equals(a1) && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.addAll(SCHEMATIC_DIFFICULTIES);
            } else if (("forcejoin".equals(a1) || "forceleave".equals(a1)) && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                completions.add("everyone");
                if ("forcejoin".equals(a1)) {
                    completions.add("nearest");
                }
                Bukkit.getOnlinePlayers().forEach(pl -> completions.add(pl.getName()));
            } else if ("recoverinventory".equals(a1) && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                Bukkit.getOnlinePlayers().forEach(pl -> completions.add(pl.getName()));
            }
            return filter(args[1], completions);
        }

        if (args.length == 3 && sender.hasPermission(ParkourOption.ADMIN.permission)) {
            String a1 = args[0].toLowerCase();
            String a2 = args[1].toLowerCase();
            if ("schematic".equals(a1)) {
                if ("paste".equals(a2)) {
                    addLoadedSchematicIds(completions);
                } else if ("create".equals(a2)) {
                    completions.addAll(SCHEMATIC_DIFFICULTIES);
                }
            }
            return filter(args[2], completions);
        }

        if (args.length == 4 && sender.hasPermission(ParkourOption.ADMIN.permission)) {
            String a1 = args[0].toLowerCase();
            String a2 = args[1].toLowerCase();
            if ("schematic".equals(a1) && "create".equals(a2)) {
                completions.addAll(SCHEMATIC_DIFFICULTIES);
            }
            return filter(args[3], completions);
        }

        return Collections.emptyList();
    }

    private static void addLoadedSchematicIds(@NotNull List<String> completions) {
        SchematicManager manager = LoParkour.getSchematicManager();
        if (manager == null) {
            return;
        }
        for (String id : manager.getAll().keySet()) {
            completions.add(id);
            String shortId = SchematicManager.configKey(id);
            if (!shortId.equalsIgnoreCase(id)) {
                completions.add(shortId);
            }
        }
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
