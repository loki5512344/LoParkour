package dev.loki.loparkour.adaptive.storage;

import dev.loki.loparkour.adaptive.model.PlayerMetrics;
import dev.loki.loparkour.adaptive.model.SkillRating;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Repository interface for storing and retrieving player statistics.
 * Implementations can use SQL or file-based storage.
 */
public interface StatsRepository {

    /**
     * Loads skill rating for a player.
     *
     * @param playerUuid The player's UUID
     * @return SkillRating or null if not found
     */
    @Nullable
    SkillRating loadSkillRating(@NotNull UUID playerUuid);

    /**
     * Saves skill rating for a player.
     *
     * @param rating The skill rating to save
     */
    void saveSkillRating(@NotNull SkillRating rating);

    /**
     * Loads detailed metrics for a player.
     *
     * @param playerUuid The player's UUID
     * @return PlayerMetrics or null if not found
     */
    @Nullable
    PlayerMetrics loadMetrics(@NotNull UUID playerUuid);

    /**
     * Saves detailed metrics for a player.
     *
     * @param metrics The metrics to save
     */
    void saveMetrics(@NotNull PlayerMetrics metrics);

    /**
     * Increments total jumps counter for a player.
     *
     * @param playerUuid The player's UUID
     * @param amount Amount to increment
     */
    void incrementJumps(@NotNull UUID playerUuid, int amount);

    /**
     * Increments total falls counter for a player.
     *
     * @param playerUuid The player's UUID
     * @param amount Amount to increment
     */
    void incrementFalls(@NotNull UUID playerUuid, int amount);

    /**
     * Updates longest streak for a player if new streak is longer.
     *
     * @param playerUuid The player's UUID
     * @param streak The new streak value
     */
    void updateLongestStreak(@NotNull UUID playerUuid, int streak);

    /**
     * Closes the repository and releases resources.
     */
    void close();
}
