package dev.loki.loparkour.adaptive.storage;

import dev.loki.loparkour.adaptive.model.PlayerMetrics;
import dev.loki.loparkour.adaptive.model.SkillRating;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * No-op implementation of StatsRepository.
 * Used as a placeholder until the full storage implementation is ready.
 *
 * This allows MetricsCollector to function without throwing errors,
 * but metrics will not be persisted between server restarts.
 */
public class NoOpStatsRepository implements StatsRepository {

    @Override
    @Nullable
    public SkillRating loadSkillRating(@NotNull UUID playerUuid) {
        return null;
    }

    @Override
    public void saveSkillRating(@NotNull SkillRating rating) {
        // No-op
    }

    @Override
    @Nullable
    public PlayerMetrics loadMetrics(@NotNull UUID playerUuid) {
        return null;
    }

    @Override
    public void saveMetrics(@NotNull PlayerMetrics metrics) {
        // No-op
    }

    @Override
    public void incrementJumps(@NotNull UUID playerUuid, int amount) {
        // No-op
    }

    @Override
    public void incrementFalls(@NotNull UUID playerUuid, int amount) {
        // No-op
    }

    @Override
    public void updateLongestStreak(@NotNull UUID playerUuid, int streak) {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }
}
