package dev.loki.loparkour.adaptive.core;

import dev.loki.loparkour.adaptive.model.SkillRating;
import org.jetbrains.annotations.NotNull;

/**
 * Calculates target difficulty based on player skill rating.
 * Converts skill ratings into normalized difficulty values (0.0-1.0).
 *
 * Formula: difficulty = min(1.0, rating / 2.0)
 * - Rating 0.0 = 0.0 difficulty (easiest)
 * - Rating 1.0 = 0.5 difficulty (medium)
 * - Rating 2.0+ = 1.0 difficulty (hardest)
 *
 * Thread-safe: Stateless, all methods are pure functions.
 */
public class DifficultyCalculator {

    private static final double MAX_RATING_FOR_FULL_DIFFICULTY = 2.0;
    private static final double MIN_DIFFICULTY = 0.0;
    private static final double MAX_DIFFICULTY = 1.0;

    /**
     * Calculates the target difficulty for a player based on their skill rating.
     *
     * @param skillRating The player's skill rating
     * @return Target difficulty (0.0-1.0)
     */
    public double calculateTargetDifficulty(@NotNull SkillRating skillRating) {
        double rating = skillRating.getRating();
        double confidence = skillRating.getConfidence();

        // Calculate base difficulty from rating
        double baseDifficulty = calculateBaseDifficulty(rating);

        // Apply confidence adjustment (lower confidence = more conservative difficulty)
        double adjustedDifficulty = applyConfidenceAdjustment(baseDifficulty, confidence);

        return clampDifficulty(adjustedDifficulty);
    }

    /**
     * Calculates base difficulty from skill rating.
     * Linear mapping: rating 2.0 = difficulty 1.0
     *
     * @param rating The skill rating
     * @return Base difficulty (0.0-1.0+)
     */
    private double calculateBaseDifficulty(double rating) {
        return rating / MAX_RATING_FOR_FULL_DIFFICULTY;
    }

    /**
     * Applies confidence adjustment to difficulty.
     * Low confidence ratings result in more conservative difficulty.
     *
     * @param baseDifficulty The base difficulty
     * @param confidence     The confidence level (0.0-1.0)
     * @return Adjusted difficulty
     */
    private double applyConfidenceAdjustment(double baseDifficulty, double confidence) {
        // If confidence is low, reduce difficulty to be safe
        // confidence 0.0 = 50% of base difficulty
        // confidence 1.0 = 100% of base difficulty
        double confidenceMultiplier = 0.5 + (confidence * 0.5);
        return baseDifficulty * confidenceMultiplier;
    }

    /**
     * Clamps difficulty to valid range [0.0, 1.0].
     *
     * @param difficulty The difficulty value
     * @return Clamped difficulty
     */
    private double clampDifficulty(double difficulty) {
        return Math.max(MIN_DIFFICULTY, Math.min(MAX_DIFFICULTY, difficulty));
    }

    /**
     * Calculates the recommended rating for a target difficulty.
     * Inverse of calculateTargetDifficulty (without confidence adjustment).
     *
     * @param targetDifficulty The desired difficulty (0.0-1.0)
     * @return Recommended skill rating
     */
    public double calculateRecommendedRating(double targetDifficulty) {
        double clampedDifficulty = clampDifficulty(targetDifficulty);
        return clampedDifficulty * MAX_RATING_FOR_FULL_DIFFICULTY;
    }

    /**
     * Determines if a player is ready for increased difficulty.
     * Checks if rating is significantly above current difficulty level.
     *
     * @param skillRating       The player's skill rating
     * @param currentDifficulty The current difficulty (0.0-1.0)
     * @return true if player is ready for harder content
     */
    public boolean isReadyForIncrease(@NotNull SkillRating skillRating, double currentDifficulty) {
        double targetDifficulty = calculateTargetDifficulty(skillRating);
        double threshold = 0.1; // 10% increase threshold

        return targetDifficulty > currentDifficulty + threshold
                && skillRating.getConfidence() >= 0.5; // Require reasonable confidence
    }

    /**
     * Determines if difficulty should be decreased for a player.
     * Checks if rating is significantly below current difficulty level.
     *
     * @param skillRating       The player's skill rating
     * @param currentDifficulty The current difficulty (0.0-1.0)
     * @return true if difficulty should be reduced
     */
    public boolean shouldDecrease(@NotNull SkillRating skillRating, double currentDifficulty) {
        double targetDifficulty = calculateTargetDifficulty(skillRating);
        double threshold = 0.15; // 15% decrease threshold (more aggressive than increase)

        return targetDifficulty < currentDifficulty - threshold;
    }

    /**
     * Calculates a smooth difficulty transition.
     * Prevents sudden difficulty spikes by limiting change rate.
     *
     * @param currentDifficulty The current difficulty
     * @param targetDifficulty  The target difficulty
     * @param maxChange         Maximum allowed change per adjustment
     * @return Smoothed difficulty
     */
    public double smoothTransition(double currentDifficulty, double targetDifficulty, double maxChange) {
        double difference = targetDifficulty - currentDifficulty;

        if (Math.abs(difference) <= maxChange) {
            return targetDifficulty;
        }

        // Move towards target by maxChange
        return currentDifficulty + Math.signum(difference) * maxChange;
    }
}
