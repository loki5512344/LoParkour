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
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;
import dev.loki.loparkour.util.ColorUtil;
import io.papermc.lib.PaperLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
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

    /**
     * Registers a player. This registers the player internally.
     * This automatically unregisters the player if it is already registered.
     *
     * @param player The player
     * @return the ParkourPlayer instance of the newly joined player
     */
    public static @NotNull ParkourPlayer register(@NotNull Player player, @NotNull Session session) {
        PreviousData data = null;
        ParkourUser existing = getUser(player);

        if (existing != null) {
            LoParkour.log("Registering player %s with existing data".formatted(player.getName()));

            data = existing.previousData;
            unregister(existing, false, false, false);
        } else {
            LoParkour.log("Registering player %s".formatted(player.getName()));
        }
        ParkourPlayer pp = new ParkourPlayer(player, session, data);

        // stats
        joinCount++;
        new ParkourJoinEvent(pp).call();

        Storage.readPlayer(pp);
        return pp;
    }

    /**
     * This is the same as {@link #leave(ParkourUser)}, but instead for a Bukkit player instance.
     *
     * @param player The Bukkit player instance that will be removed from the game if the player is active.
     * @see #leave(ParkourUser)
     */
    public static void leave(@NotNull Player player) {
        ParkourUser user = getUser(player);
        if (user == null) {
            return;
        }
        leave(user);
    }

    /**
     * Forces user to leave. Follows behaviour of /parkour leave.
     *
     * @param user The user.
     */
    public static void leave(@NotNull ParkourUser user) {
        unregister(user, true, true, false);
    }

    /**
     * Unregisters a Parkour user instance.
     *
     * @param user                The user to unregister.
     * @param restorePreviousData Whether to restore the data from before the player joined the parkour.
     * @param kickIfBungee        Whether to kick the player if Bungeecord mode is enabled.
     */
    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean urgent) {
        new ParkourLeaveEvent(user).call();
        LoParkour.log("Unregistering player %s, restorePreviousData = %s, kickIfBungee = %s".formatted(user.getName(), restorePreviousData, kickIfBungee));

        try {
            user.unregister();

            // TODO: Implement scoreboard deletion with LoLib
            // if (user.board != null) {
            //     user.board.delete();
            // }
        } catch (Exception ex) { // safeguard to prevent people from losing data
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

    // Sends a player to a BungeeCord server. server is the server name.
    private static void sendPlayerToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        try {
            player.sendPluginMessage(LoParkour.getPlugin(), "BungeeCord", out.toByteArray());
        } catch (ChannelNotRegisteredException ex) {
            LoParkour.getPlugin().getLogger().severe("Error while trying to send %s to server %s. This server is not registered.".formatted(player.getName(), server) + " - " + ex.getMessage());
            player.kickPlayer("Couldn't move you to %s. Please rejoin.".formatted(server));
        }
    }

    /**
     * @param player The player.
     * @return True when this player is a {@link ParkourUser}, false if not.
     */
    public static boolean isUser(@Nullable Player player) {
        return player != null && getUsers().stream().anyMatch(other -> other.player == player);
    }

    /**
     * @param player The player.
     * @return player as a {@link ParkourUser}, null if not found.
     */
    public static @Nullable ParkourUser getUser(@NotNull Player player) {
        return getUsers().stream()
                .filter(other -> other.getUUID() == player.getUniqueId())
                .findAny()
                .orElse(null);
    }

    /**
     * @return Set with all users.
     */
    public static Set<ParkourUser> getUsers() {
        return Divider.sections.keySet().stream()
                .flatMap(session -> session.getUsers().stream())
                .collect(Collectors.toSet());
    }

    /**
     * This user's locale
     */
    @NotNull
    public String locale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);

    /**
     * This user's scoreboard
     */
    public Scoreboard board;

    /**
     * This user's PreviousData
     */
    @NotNull
    public PreviousData previousData;

    /**
     * The selected {@link Session.ChatType}
     */
    public Session.ChatType chatType = Session.ChatType.PUBLIC;

    /**
     * The {@link Session} this user is in.
     */
    public final Session session;

    /**
     * The Bukkit player instance associated with this user.
     */
    public final Player player;

    /**
     * The {@link Instant} when the player joined.
     */
    public final Instant joined;

    /**
     * The amount of players that have joined while the plugin has been enabled.
     */
    public static int joinCount;

    public ParkourUser(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        this.player = player;
        this.session = session;
        this.joined = Instant.now();
        this.previousData = previousData == null ? new PreviousData(player) : previousData;

        if (Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD))) {
            this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        }
    }

    /**
     * Unregisters this user.
     */
    protected abstract void unregister();

    /**
     * Teleports the player asynchronously.
     *
     * @param to Where the player will be teleported to
     */
    public void teleport(@NotNull Location to) {
        PaperLib.teleportAsync(player, to);
    }

    /**
     * Sends a message.
     *
     * @param message The message
     */
    public void send(String message) {
        player.sendMessage(ColorUtil.color(message));
    }

    /**
     * Sends a translated message
     *
     * @param key    The translation key
     * @param format Any objects that may be given to the formatting of the string.
     */
    public void sendTranslated(String key, Object... format) {
        send(Locales.getString(locale, key).formatted(format));
    }

    /**
     * Updates the scoreboard for the specified generator.
     *
     * @param generator The generator.
     */
    public void updateScoreboard(ParkourGenerator generator) {
        // board can be null a few ticks after on player leave
        if (board == null || false || !generator.profile.get("showScoreboard").asBoolean()) {
            return;
        }

        Leaderboard leaderboard = generator.getMode().getLeaderboard();
        Score top = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.getScoreAtRank(1);
        Score high = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.get(getUUID());
        if (top == null) {
            top = new Score("?", "?", "?", 0);
        }

        // board.updateTitle(replace(Locales.getString(locale, "scoreboard.title"), top, high, generator));
        // board.updateLines(replace(Locales.getStringList(locale, "scoreboard.lines"), top, high, generator));
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

    // translate papi
    private String translate(Player player, String string) {
        return LoParkour.getPlaceholderHook() == null ? string : PlaceholderAPI.setPlaceholders(player, string);
    }

    /**
     * @return The player's uuid
     */
    public UUID getUUID() {
        return player.getUniqueId();
    }

    /**
     * @return The player's location
     */
    public Location getLocation() {
        return player.getLocation();
    }

    /**
     * @return The player's name
     */
    public String getName() {
        return player.getName();
    }

    /**
     * @param player The player
     * @return true if the player is a Bedrock player, false if not.
     */
    public static boolean isBedrockPlayer(Player player) {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateHook.isBedrockPlayer(player);
    }
}
