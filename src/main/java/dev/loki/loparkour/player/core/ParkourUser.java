package dev.loki.loparkour.player.core;
import dev.loki.loparkour.player.service.UserRegistry;
import dev.loki.loparkour.player.service.ScoreboardManager;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.hook.floodgate.FloodgateHook;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.player.data.PreviousData;
import dev.loki.loparkour.session.core.Session;
import dev.loki.loparkour.util.text.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Superclass of every type of player. This encompasses every player currently in the Parkour world.
 * This includes active players ({@link ParkourPlayer}) and spectators ({@link ParkourSpectator}).
 *
 * @author loki
 */
public abstract class ParkourUser {

    public static @NotNull ParkourPlayer register(@NotNull Player player, @NotNull Session session) {
        return UserRegistry.register(player, session);
    }

    public static void leave(@NotNull Player player) {
        UserRegistry.leave(player);
    }

    public static void leave(@NotNull ParkourUser user) {
        UserRegistry.leave(user);
    }

    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean urgent) {
        UserRegistry.unregister(user, restorePreviousData, kickIfBungee, urgent);
    }

    public static boolean isUser(@Nullable Player player) {
        return UserRegistry.isUser(player);
    }

    public static @Nullable ParkourUser getUser(@NotNull Player player) {
        return UserRegistry.getUser(player);
    }

    public static Set<ParkourUser> getUsers() {
        return UserRegistry.getUsers();
    }

    // ─── Fields ───────────────────────────────────────────────────────────────

    @NotNull public String locale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);
    @Nullable public Scoreboard board;
    @NotNull public PreviousData previousData;
    public Session.ChatType chatType = Session.ChatType.PUBLIC;
    public final Session session;
    public final Player player;
    public final Instant joined;
    private ScoreboardManager scoreboardManager;

    @SuppressWarnings("deprecation") // Scoreboard: legacy registerNewObjective(String,String,String)
    public ParkourUser(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        this.player = player;
        this.session = session;
        this.joined = Instant.now();
        this.previousData = previousData == null ? new PreviousData(player) : previousData;

        if (Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD))) {
            this.board = Bukkit.getScoreboardManager().getNewScoreboard();
            this.scoreboardManager = new ScoreboardManager(player, player.getUniqueId(), board, locale);
            
            Objective obj = this.board.registerNewObjective("lp_sidebar", "dummy",
                    ColorUtil.color(Locales.getString(locale, "scoreboard.title")));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(this.board);
        }
    }

    public abstract void unregister();

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
     */
    public void updateScoreboard(ParkourGenerator generator) {
        if (scoreboardManager != null) {
            scoreboardManager.update(generator);
        }
    }

    public UUID getUUID() {
        return player.getUniqueId();
    }

    public Location getLocation() {
        return player.getLocation();
    }

    public String getName() {
        return player.getName();
    }

    public static boolean isBedrockPlayer(Player player) {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateHook.isBedrockPlayer(player);
    }
}
