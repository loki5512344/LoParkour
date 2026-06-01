package dev.loki.loparkour.adaptive.model;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores detailed player performance metrics for adaptive difficulty calculation.
 * Tracks timing, jump patterns, and near-miss statistics.
 */
public class PlayerMetrics {

    private final UUID playerUuid;
    private double avgTimePerBlock;
    private final Map<String, Integer> jumpTypeStats;
    private int nearMissCount;
    private int totalJumps;
    private long lastUpdated;

    public PlayerMetrics(@NotNull UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.avgTimePerBlock = 0.0;
        this.jumpTypeStats = new HashMap<>();
        this.nearMissCount = 0;
        this.totalJumps = 0;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Records a successful jump with timing data.
     *
     * @param jumpType The type of jump (e.g., "normal", "diagonal", "momentum")
     * @param timeMs Time taken for this jump in milliseconds
     */
    public void recordJump(@NotNull String jumpType, long timeMs) {
        jumpTypeStats.merge(jumpType, 1, Integer::sum);
        totalJumps++;

        // Update rolling average
        if (avgTimePerBlock == 0.0) {
            avgTimePerBlock = timeMs;
        } else {
            avgTimePerBlock = (avgTimePerBlock * 0.9) + (timeMs * 0.1);
        }

        lastUpdated = System.currentTimeMillis();
    }

    /**
     * Records a near-miss event (landed close to block edge).
     */
    public void recordNearMiss() {
        nearMissCount++;
        lastUpdated = System.currentTimeMillis();
    }

    /**
     * Gets the frequency of a specific jump type.
     *
     * @param jumpType The jump type to query
     * @return Percentage (0.0 to 1.0) of total jumps
     */
    public double getJumpTypeFrequency(@NotNull String jumpType) {
        if (totalJumps == 0) {
            return 0.0;
        }
        return jumpTypeStats.getOrDefault(jumpType, 0) / (double) totalJumps;
    }

    /**
     * Gets near-miss rate as percentage of total jumps.
     *
     * @return Near-miss rate (0.0 to 1.0)
     */
    public double getNearMissRate() {
        if (totalJumps == 0) {
            return 0.0;
        }
        return nearMissCount / (double) totalJumps;
    }

    /**
     * Calculates success rate based on total jumps.
     * Assumes all recorded jumps are successful (falls are tracked separately).
     *
     * @return Success rate (0.0 to 1.0)
     */
    public double getSuccessRate() {
        // Since we only record successful jumps, success rate is based on
        // the assumption that player has some failures not tracked here
        // For now, return 1.0 if we have jumps, or use near-miss rate as proxy
        if (totalJumps == 0) {
            return 0.0;
        }
        // High near-miss rate indicates good performance
        return Math.min(1.0, 0.7 + (getNearMissRate() * 0.3));
    }

    /**
     * Gets the number of successful jumps (same as total jumps for now).
     *
     * @return Number of successful jumps
     */
    public int getSuccessfulJumps() {
        return totalJumps;
    }

    // Getters

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public double getAvgTimePerBlock() {
        return avgTimePerBlock;
    }

    @NotNull
    public Map<String, Integer> getJumpTypeStats() {
        return new HashMap<>(jumpTypeStats);
    }

    public int getNearMissCount() {
        return nearMissCount;
    }

    public int getTotalJumps() {
        return totalJumps;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    // Setters for deserialization

    public void setAvgTimePerBlock(double avgTimePerBlock) {
        this.avgTimePerBlock = avgTimePerBlock;
    }

    public void setNearMissCount(int nearMissCount) {
        this.nearMissCount = nearMissCount;
    }

    public void setTotalJumps(int totalJumps) {
        this.totalJumps = totalJumps;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void putJumpTypeStat(@NotNull String jumpType, int count) {
        jumpTypeStats.put(jumpType, count);
    }
}
