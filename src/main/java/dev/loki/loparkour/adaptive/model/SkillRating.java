package dev.loki.loparkour.adaptive.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a player's skill rating with confidence level.
 * Used for adaptive difficulty calculation.
 */
public class SkillRating {

    private final UUID playerUuid;
    private double rating;
    private double confidence;
    private int sessionsCount;
    private long lastUpdated;

    /**
     * Creates a new skill rating with default values.
     *
     * @param playerUuid The player's UUID
     */
    public SkillRating(@NotNull UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.rating = 1.0;
        this.confidence = 0.0;
        this.sessionsCount = 0;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Creates a skill rating with specific values.
     *
     * @param playerUuid The player's UUID
     * @param rating The skill rating (typically 0.5 to 2.0)
     * @param confidence The confidence level (0.0 to 1.0)
     * @param sessionsCount Number of sessions used to calculate this rating
     */
    public SkillRating(@NotNull UUID playerUuid, double rating, double confidence, int sessionsCount) {
        this.playerUuid = playerUuid;
        this.rating = clampRating(rating);
        this.confidence = clampConfidence(confidence);
        this.sessionsCount = sessionsCount;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Updates the skill rating based on performance.
     *
     * @param performanceScore Performance score from recent session (0.0 to 2.0)
     * @param weight Weight of this update (0.0 to 1.0)
     */
    public void updateRating(double performanceScore, double weight) {
        rating = (rating * (1.0 - weight)) + (performanceScore * weight);
        rating = clampRating(rating);

        sessionsCount++;
        confidence = Math.min(1.0, confidence + 0.05);

        lastUpdated = System.currentTimeMillis();
    }

    /**
     * Gets the effective rating adjusted by confidence.
     * Low confidence ratings are pulled toward 1.0 (neutral).
     *
     * @return Confidence-adjusted rating
     */
    public double getEffectiveRating() {
        return (rating * confidence) + (1.0 * (1.0 - confidence));
    }

    /**
     * Checks if this rating is reliable enough for adaptive difficulty.
     *
     * @return True if confidence is above threshold
     */
    public boolean isReliable() {
        return confidence >= 0.3 && sessionsCount >= 3;
    }

    private double clampRating(double value) {
        return Math.max(0.5, Math.min(2.0, value));
    }

    private double clampConfidence(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public double getRating() {
        return rating;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getSessionsCount() {
        return sessionsCount;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setRating(double rating) {
        this.rating = clampRating(rating);
    }

    public void setConfidence(double confidence) {
        this.confidence = clampConfidence(confidence);
    }

    public void setSessionsCount(int sessionsCount) {
        this.sessionsCount = sessionsCount;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return String.format("SkillRating{rating=%.2f, confidence=%.2f, sessions=%d}",
                rating, confidence, sessionsCount);
    }
}
