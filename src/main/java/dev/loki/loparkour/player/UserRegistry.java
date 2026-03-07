package dev.loki.loparkour.player;

import dev.loki.loparkour.api.event.ParkourJoinEvent;
import dev.loki.loparkour.api.event.ParkourLeaveEvent;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.data.PreviousData;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.world.Divider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages registration and unregistration of parkour users.
 *
 * @since 5.0.0
 */
public class UserRegistry {

    private static int joinCount = 0;

    public static @NotNull ParkourPlayer register(@NotNull Player player, @NotNull Session session) {
        PreviousData data = null;
        ParkourUser existing = getUser(player);

        if (existing != null) {
            data = existing.previousData;
            unregister(existing, false, false, false);
        }

        ParkourPlayer pp = new ParkourPlayer(player, session, data);
        joinCount++;
        new ParkourJoinEvent(pp).call();
        Storage.readPlayer(pp);

        return pp;
    }

    public static void leave(@NotNull Player player) {
        ParkourUser user = getUser(player);
        if (user != null) {
            leave(user);
        }
    }

    public static void leave(@NotNull ParkourUser user) {
        unregister(user, true, true, false);
    }

    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean urgent) {
        new ParkourLeaveEvent(user).call();

        try {
            user.unregister();
            resetScoreboard(user);
        } catch (Exception ex) {
            handleUnregisterError(user, ex);
        }

        if (restorePreviousData && shouldKickToBungee(kickIfBungee)) {
            BungeeUtil.sendPlayerToServer(user.player, Config.CONFIG.getString("bungeecord.return_server"));
            return;
        }

        if (restorePreviousData) {
            restorePreviousState(user, urgent);
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

    public static int getJoinCount() {
        return joinCount;
    }

    private static void resetScoreboard(ParkourUser user) {
        if (user.board != null) {
            user.player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            user.board = null;
        }
    }

    private static void handleUnregisterError(ParkourUser user, Exception ex) {
        user.player.getServer().getLogger().log(java.util.logging.Level.SEVERE,
                "Error while trying to make player " + user.getName() + " leave", ex);
        user.send("<red><bold>There was an error while trying to handle leaving.");
    }

    private static boolean shouldKickToBungee(boolean kickIfBungee) {
        return Config.CONFIG.getBoolean("bungeecord.enabled") && kickIfBungee;
    }

    private static void restorePreviousState(ParkourUser user, boolean urgent) {
        user.previousData.apply(user.player, urgent);

        Mode mode = user.session.generator.getMode();
        if (mode == null) {
            mode = Modes.DEFAULT;
        }

        if (user instanceof ParkourPlayer player) {
            Mode finalMode = mode;
            user.previousData.onLeave.forEach(r -> r.execute(player, finalMode));
        }
    }
}
