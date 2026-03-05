package dev.loki.loparkour.storage;

import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * I/O handling.
 *
 * @author loki
 * @since 5.0.0
 */
public class Storage {

    public static void init(String mode) {
        if (Option.SQL) {
            StorageSQL.init(mode);
        }
    }

    /**
     * If SQL is enabled, defers the callback until the connection is ready.
     * If SQL is disabled (disk), calls the callback immediately.
     */
    public static void runWhenReady(Runnable callback) {
        if (Option.SQL) {
            StorageSQL.runWhenConnected(callback);
        } else {
            callback.run();
        }
    }

    public static void close() {
        if (Option.SQL) {
            StorageSQL.close();
        }
    }

    /**
     * Reads scores.
     *
     * @param mode The mode.
     * @return Map with all scores, unsorted.
     */
    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        if (Option.SQL) {
            return StorageSQL.readScores(mode);
        } else {
            return StorageDisk.readScores(mode);
        }
    }

    /**
     * Writes scores.
     *
     * @param mode   The mode.
     * @param scores The score map.
     */
    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        if (Option.SQL) {
            StorageSQL.writeScores(mode, scores);
        } else {
            StorageDisk.writeScores(mode, scores);
        }
    }

    /**
     * Reads player data and applies changes.
     *
     * @param player The player.
     */
    public static void readPlayer(@NotNull ParkourPlayer player) {
        if (Option.SQL) {
            StorageSQL.readPlayer(player);
        } else {
            StorageDisk.readPlayer(player);
        }
    }

    /**
     * Writes player data.
     *
     * @param player The player.
     */
    public static void writePlayer(@NotNull ParkourPlayer player) {
        if (Option.SQL) {
            StorageSQL.writePlayer(player);
        } else {
            StorageDisk.writePlayer(player);
        }
    }
}
