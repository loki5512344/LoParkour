package dev.loki.loparkour.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourJoinEvent;
import dev.loki.loparkour.api.event.ParkourLeaveEvent;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.hook.FloodgateHook;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.data.PreviousData;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.world.Divider;
import dev.loki.loparkour.util.ColorUtil;
import io.papermc.lib.PaperLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Superclass of every type of player. This encompasses every player currently in the Parkour world.
 * This includes active players ({@link ParkourPlayer}) and spectators ({@link ParkourSpectator}).
 *
 * @author loki
 */
public abstract class ParkourUser {

    public static @NotNull ParkourPlayer register(@NotNull Player player, @NotNull Session session) {
        PreviousData data = null;
        ParkourUser existing = getUser(player);

        if (existing != null) {
            data = existing.previousData;
            unregister(existing, false, false, false);
        } else {
        }
        ParkourPlayer pp = new ParkourPlayer(player, session, data);

        joinCount++;
        new ParkourJoinEvent(pp).call();

        Storage.readPlayer(pp);
        return pp;
    }

    public static void leave(@NotNull Player player) {
        ParkourUser user = getUser(player);
        if (user == null) return;
        leave(user);
    }

    public static void leave(@NotNull ParkourUser user) {
        unregister(user, true, true, false);
    }

    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean urgent) {
        new ParkourLeaveEvent(user).call();

        try {
            user.unregister();

            // Reset scoreboard to main
            if (user.board != null) {
                user.player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                user.board = null;
            }
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "Error while trying to make player " + user.getName() + " leave", ex);
            user.send("<red><bold>There was an error while trying to handle leaving.");
        }

        if (restorePreviousData && Config.CONFIG.getBoolean("bungeecord.enabled") && kickIfBungee) {
            sendPlayerToServer(user.player, Config.CONFIG.getString("bungeecord.return_server"));
            return;
        }

        if (!restorePreviousData) return;

        user.previousData.apply(user.player, urgent);

        Mode mode = user.session.generator.getMode();
        if (mode == null) {
            LoParkour.getPlugin().getLogger().severe("Mode is null for %s".formatted(user.getName()));
            mode = Modes.DEFAULT;
        }

        if (user instanceof ParkourPlayer player) {
            Mode finalMode = mode;
            user.previousData.onLeave.forEach(r -> r.execute(player, finalMode));
        }
    }

    private static void sendPlayerToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        try {
            player.sendPluginMessage(LoParkour.getPlugin(), "BungeeCord", out.toByteArray());
        } catch (ChannelNotRegisteredException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while trying to send %s to server %s. - %s".formatted(player.getName(), server, ex.getMessage()));
            player.kickPlayer("Couldn't move you to %s. Please rejoin.".formatted(server));
        }
    }

    public static boolean isUser(@Nullable Player player) {
        return player != null && getUsers().stream().anyMatch(other -> other.player == player);
    }

    public static @Nullable ParkourUser getUser(@NotNull Player player) {
        return getUsers().stream()
                .filter(other -> other.getUUID().equals(player.getUniqueId()))
                .findAny()
                .orElse(null);
    }

    public static Set<ParkourUser> getUsers() {
        return Divider.sections.keySet().stream()
                .flatMap(session -> session.getUsers().stream())
                .collect(Collectors.toSet());
    }

    // ─── Fields ───────────────────────────────────────────────────────────────

    @NotNull public String locale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);
    @Nullable public Scoreboard board;
    @NotNull public PreviousData previousData;
    public Session.ChatType chatType = Session.ChatType.PUBLIC;
    public final Session session;
    public final Player player;
    public final Instant joined;
    public static int joinCount;

    public ParkourUser(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        this.player = player;
        this.session = session;
        this.joined = Instant.now();
        this.previousData = previousData == null ? new PreviousData(player) : previousData;

        if (Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD))) {
            this.board = Bukkit.getScoreboardManager().getNewScoreboard();
            // Create the sidebar objective immediately and assign to player
            Objective obj = this.board.registerNewObjective("lp_sidebar", "dummy",
                    ColorUtil.color(Locales.getString(locale, "scoreboard.title")));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(this.board);
        }
    }

    protected abstract void unregister();

    public void teleport(@NotNull Location to) {
        player.teleport(to);
    }

    public void send(String message) {
        player.sendMessage(ColorUtil.color(message));
    }

    public void sendTranslated(String key, Object... format) {
        send(Locales.getString(locale, key).formatted(format));
    }

    /**
     * Updates the sidebar scoreboard for the given generator state.
     * Uses vanilla Bukkit scoreboard API — Team prefix trick for coloured lines.
     */
    public void updateScoreboard(ParkourGenerator generator) {
        if (board == null || !generator.profile.get("showScoreboard").asBoolean()) {
            return;
        }

        Leaderboard leaderboard = generator.getMode().getLeaderboard();
        Score top = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.getScoreAtRank(1);
        Score high = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.get(getUUID());
        if (top == null) top = new Score("?", "?", "?", 0);

        // Update title
        Objective obj = board.getObjective("lp_sidebar");
        if (obj == null) {
            obj = board.registerNewObjective("lp_sidebar", "dummy",
                    ColorUtil.color(replace(Locales.getString(locale, "scoreboard.title"), top, high, generator)));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.setDisplayName(ColorUtil.color(
                    replace(Locales.getString(locale, "scoreboard.title"), top, high, generator)));
        }

        // Update lines using Team prefix trick (each line = unique entry).
        // We need globally-unique entries: use a combination of colour codes that gives
        // up to 32 unique strings (§0§0, §0§1, … §1§0 …). This safely covers any
        // reasonable scoreboard length.
        List<String> lines = replace(Locales.getStringList(locale, "scoreboard.lines"), top, high, generator);
        String[] codes = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
        for (int i = 0; i < lines.size(); i++) {
            String entry = "§" + codes[i / codes.length % codes.length] + "§" + codes[i % codes.length];
            String teamName = "lp_line_" + i;

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.addEntry(entry);
            }

            team.setPrefix(ColorUtil.color(lines.get(i)));
            obj.getScore(entry).setScore(lines.size() - i);
        }
    }

    private List<String> replace(List<String> s, Score top, Score high, ParkourGenerator generator) {
        return s.stream().map(line -> replace(line, top, high, generator)).toList();
    }

    private String replace(String s, Score top, Score high, ParkourGenerator generator) {
        return ColorUtil.color(translate(player, s)
                .replace("%score%", Integer.toString(generator.score))
                .replace("%time%", generator.getFormattedTime())
                .replace("%difficulty%", Double.toString(generator.getDifficultyScore()))
                .replace("%top_score%", Integer.toString(top.score()))
                .replace("%top_player%", top.name())
                .replace("%top_time%", top.time())
                .replace("%high_score%", Integer.toString(high.score()))
                .replace("%high_score_time%", high.time()));
    }

    private String translate(Player player, String string) {
        return LoParkour.getPlaceholderHook() == null ? string : PlaceholderAPI.setPlaceholders(player, string);
    }

    public UUID getUUID() { return player.getUniqueId(); }
    public Location getLocation() { return player.getLocation(); }
    public String getName() { return player.getName(); }

    public static boolean isBedrockPlayer(Player player) {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateHook.isBedrockPlayer(player);
    }
}
