package dev.loki.loparkour.adaptive.storage;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.adaptive.model.PlayerMetrics;
import dev.loki.loparkour.adaptive.model.SkillRating;
import dev.loki.loparkour.storage.sql.SQLConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL-based implementation of StatsRepository using loparkour_player_stats table.
 * Uses batch updates for optimization.
 */
public class SQLStatsStorage implements StatsRepository {

    private final SQLConnectionManager connectionManager;
    private final ConcurrentHashMap<UUID, PendingUpdate> pendingUpdates;
    private static final int BATCH_SIZE = 50;

    public SQLStatsStorage(@NotNull SQLConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.pendingUpdates = new ConcurrentHashMap<>();
    }

    @Override
    @Nullable
    public SkillRating loadSkillRating(@NotNull UUID playerUuid) {
        String query = "SELECT skill_rating, sessions_count FROM loparkour_player_stats WHERE player_uuid = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double rating = rs.getDouble("skill_rating");
                int sessions = rs.getInt("sessions_count");
                double confidence = Math.min(1.0, sessions * 0.05);
                return new SkillRating(playerUuid, rating, confidence, sessions);
            }
        } catch (SQLException e) {
            LoParkour.getPlugin().getLogger().warning("Failed to load skill rating: " + e.getMessage());
        }

        return null;
    }

    @Override
    public void saveSkillRating(@NotNull SkillRating rating) {
        String query = "INSERT INTO loparkour_player_stats (player_uuid, skill_rating, sessions_count) " +
                       "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE skill_rating = ?, sessions_count = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, rating.getPlayerUuid().toString());
            stmt.setDouble(2, rating.getRating());
            stmt.setInt(3, rating.getSessionsCount());
            stmt.setDouble(4, rating.getRating());
            stmt.setInt(5, rating.getSessionsCount());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LoParkour.getPlugin().getLogger().warning("Failed to save skill rating: " + e.getMessage());
        }
    }

    @Override
    @Nullable
    public PlayerMetrics loadMetrics(@NotNull UUID playerUuid) {
        // Metrics are stored in JSON files, not SQL
        return null;
    }

    @Override
    public void saveMetrics(@NotNull PlayerMetrics metrics) {
        // Metrics are stored in JSON files, not SQL
    }

    @Override
    public void incrementJumps(@NotNull UUID playerUuid, int amount) {
        PendingUpdate update = pendingUpdates.computeIfAbsent(playerUuid, k -> new PendingUpdate());
        update.jumps += amount;

        if (pendingUpdates.size() >= BATCH_SIZE) {
            flushBatch();
        }
    }

    @Override
    public void incrementFalls(@NotNull UUID playerUuid, int amount) {
        PendingUpdate update = pendingUpdates.computeIfAbsent(playerUuid, k -> new PendingUpdate());
        update.falls += amount;

        if (pendingUpdates.size() >= BATCH_SIZE) {
            flushBatch();
        }
    }

    @Override
    public void updateLongestStreak(@NotNull UUID playerUuid, int streak) {
        PendingUpdate update = pendingUpdates.computeIfAbsent(playerUuid, k -> new PendingUpdate());
        update.longestStreak = Math.max(update.longestStreak, streak);

        if (pendingUpdates.size() >= BATCH_SIZE) {
            flushBatch();
        }
    }

    /**
     * Flushes all pending updates to database using batch operations.
     */
    public void flushBatch() {
        if (pendingUpdates.isEmpty()) {
            return;
        }

        String query = "INSERT INTO loparkour_player_stats (player_uuid, total_jumps, total_falls, longest_streak) " +
                       "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                       "total_jumps = total_jumps + VALUES(total_jumps), " +
                       "total_falls = total_falls + VALUES(total_falls), " +
                       "longest_streak = GREATEST(longest_streak, VALUES(longest_streak))";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            List<UUID> processed = new ArrayList<>();

            for (var entry : pendingUpdates.entrySet()) {
                UUID uuid = entry.getKey();
                PendingUpdate update = entry.getValue();

                stmt.setString(1, uuid.toString());
                stmt.setInt(2, update.jumps);
                stmt.setInt(3, update.falls);
                stmt.setInt(4, update.longestStreak);
                stmt.addBatch();

                processed.add(uuid);
            }

            stmt.executeBatch();

            for (UUID uuid : processed) {
                pendingUpdates.remove(uuid);
            }

        } catch (SQLException e) {
            LoParkour.getPlugin().getLogger().warning("Failed to flush batch updates: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        flushBatch();
    }

    private static class PendingUpdate {
        int jumps = 0;
        int falls = 0;
        int longestStreak = 0;
    }
}
