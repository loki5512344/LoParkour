package dev.loki.loparkour.adaptive.core;

import dev.loki.loparkour.adaptive.model.PlayerMetrics;
import dev.loki.loparkour.adaptive.model.SkillRating;
import org.jetbrains.annotations.NotNull;

/**
 * Analyzes player metrics and calculates skill ratings.
 * Uses a formula that considers success rate, near misses, and consistency.
 *
 * Formula: rating = baseRating * (successRate^2) * (1 + nearMissBonus) * streakMultiplier
 * - baseRating: 1.0 (baseline skill level)
 * - successRate: percentage of successful jumps (0.0-1.0)
 * - nearMissBonus: 0.1 per near miss (rewards precision)
 * - streakMultiplier: 1 + (successfulJumps / 100) (rewards consistency)
 *
 * Thread-safe: Stateless, all methods are pure functions.
 */
public class SkillAnalyzer {

    private static final double BASE_RATING = 1.0;
    private static final double NEAR_MISS_BONUS = 0.1;
    private static final double STREAK_DIVISOR = 100.0;
    private static final int MIN_JUMPS_FOR_CONFIDENCE = 10;
    private static final int MAX_JUMPS_FOR_CONFIDENCE = 100;

    /**
     * Analyzes player metrics and calculates a skill rating.
     *
     * @param metrics The player's performance metrics
     * @return A skill rating with confidence level
     */
    @NotNull
    public SkillRating analyzeSkill(@NotNull PlayerMetrics metrics) {
        double successRate = metrics.getSuccessRate();
        int nearMisses = metrics.getNearMissCount();
        int successfulJumps = metrics.getSuccessfulJumps();
        int totalJumps = metrics.getTotalJumps();

        // Calculate rating components
        double successComponent = calculateSuccessComponent(successRate);
        double nearMissBonus = calculateNearMissBonus(nearMisses);
        double streakMultiplier = calculateStreakMultiplier(successfulJumps);

        // Combine components
        double rating = BASE_RATING * successComponent * nearMissBonus * streakMultiplier;

        // Calculate confidence based on sample size
        double confidence = calculateConfidence(totalJumps);

        // Sessions count is 1 for current session analysis
        int sessionsCount = 1;

        return new SkillRating(metrics.getPlayerUuid(), rating, confidence, sessionsCount);
    }

    /**
     * Calculates the success rate component of the rating.
     * Uses squared success rate to emphasize consistency.
     *
     * @param successRate The player's success rate (0.0-1.0)
     * @return Success component (0.0-1.0)
     */
    private double calculateSuccessComponent(double successRate) {
        return Math.pow(successRate, 2);
    }

    /**
     * Calculates the near miss bonus multiplier.
     * Rewards players who land close to block edges (precision).
     *
     * @param nearMisses Number of near miss landings
     * @return Near miss multiplier (1.0+)
     */
    private double calculateNearMissBonus(int nearMisses) {
        return 1.0 + (nearMisses * NEAR_MISS_BONUS);
    }

    /**
     * Calculates the streak multiplier based on successful jumps.
     * Rewards consistent performance over time.
     *
     * @param successfulJumps Number of successful jumps
     * @return Streak multiplier (1.0+)
     */
    private double calculateStreakMultiplier(int successfulJumps) {
        return 1.0 + (successfulJumps / STREAK_DIVISOR);
    }

    /**
     * Calculates confidence level based on sample size.
     * More jumps = higher confidence in the rating.
     *
     * @param totalJumps Total number of jumps attempted
     * @return Confidence level (0.0-1.0)
     */
    private double calculateConfidence(int totalJumps) {
        if (totalJumps < MIN_JUMPS_FOR_CONFIDENCE) {
            // Low confidence for small samples
            return (double) totalJumps / MIN_JUMPS_FOR_CONFIDENCE;
        } else if (totalJumps >= MAX_JUMPS_FOR_CONFIDENCE) {
            // Maximum confidence reached
            return 1.0;
        } else {
            // Linear interpolation between min and max
            double range = MAX_JUMPS_FOR_CONFIDENCE - MIN_JUMPS_FOR_CONFIDENCE;
            double progress = totalJumps - MIN_JUMPS_FOR_CONFIDENCE;
            return progress / range;
        }
    }

    /**
     * Estimates the minimum rating for a given success rate.
     * Useful for UI display and difficulty selection.
     *
     * @param successRate The success rate (0.0-1.0)
     * @return Estimated minimum rating
     */
    public double estimateMinRating(double successRate) {
        return BASE_RATING * calculateSuccessComponent(successRate);
    }

    /**
     * Estimates the maximum rating for a given success rate.
     * Assumes high near miss count and long streak.
     *
     * @param successRate The success rate (0.0-1.0)
     * @return Estimated maximum rating
     */
    public double estimateMaxRating(double successRate) {
        double successComponent = calculateSuccessComponent(successRate);
        double maxNearMissBonus = calculateNearMissBonus(50); // Assume 50 near misses
        double maxStreakMultiplier = calculateStreakMultiplier(MAX_JUMPS_FOR_CONFIDENCE);

        return BASE_RATING * successComponent * maxNearMissBonus * maxStreakMultiplier;
    }
}
