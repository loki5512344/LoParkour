package dev.loki.loparkour.adaptive.core;

import dev.loki.loparkour.generator.core.model.Profile;
import org.jetbrains.annotations.NotNull;

/**
 * Adjusts difficulty profile parameters based on target difficulty.
 * Modifies distance, height, and special block chances to match player skill.
 *
 * Difficulty scale (0.0-1.0):
 * - 0.0-0.3: Easy (short jumps, minimal height variation)
 * - 0.3-0.6: Medium (balanced jumps, moderate height)
 * - 0.6-0.8: Hard (longer jumps, more height variation)
 * - 0.8-1.0: Expert (maximum distance, high variation)
 *
 * Thread-safe: Stateless, all methods are pure functions.
 */
public class DifficultyAdjuster {

    // Distance bounds (blocks)
    private static final int MIN_DISTANCE = 1;
    private static final int MAX_DISTANCE = 4;

    // Height bounds (blocks)
    private static final int MIN_HEIGHT = -1;
    private static final int MAX_HEIGHT = 2;

    /**
     * Adjusts a profile to match the target difficulty.
     * Creates a new profile with modified parameters.
     *
     * @param baseProfile      The base profile to adjust
     * @param targetDifficulty Target difficulty (0.0-1.0)
     * @return New profile with adjusted parameters
     */
    @NotNull
    public Profile adjustProfile(@NotNull Profile baseProfile, double targetDifficulty) {
        Profile adjusted = new Profile();

        // Copy base settings
        copyBaseSettings(baseProfile, adjusted);

        // Adjust difficulty parameters
        adjustDistanceChances(adjusted, targetDifficulty);
        adjustHeightChances(adjusted, targetDifficulty);
        adjustSpecialFrequency(adjusted, targetDifficulty);

        return adjusted;
    }

    /**
     * Copies non-difficulty settings from base profile.
     */
    private void copyBaseSettings(@NotNull Profile source, @NotNull Profile target) {
        String[] settingsKeys = {"min-y", "max-y", "heading", "first-block-material"};

        for (String key : settingsKeys) {
            if (source.settings.containsKey(key)) {
                target.set(key, source.get(key).value());
            }
        }
    }

    /**
     * Adjusts distance chances based on difficulty.
     */
    private void adjustDistanceChances(@NotNull Profile profile, double difficulty) {
        double[] weights = DifficultyWeights.calculateDistanceWeights(difficulty);

        profile.set("distance.min", String.valueOf(MIN_DISTANCE));
        profile.set("distance.max", String.valueOf(MAX_DISTANCE));

        for (int distance = MIN_DISTANCE; distance <= MAX_DISTANCE; distance++) {
            int index = distance - MIN_DISTANCE;
            int chance = (int) Math.round(weights[index] * 100);
            profile.set("distance.chances." + distance, String.valueOf(chance));
        }
    }

    /**
     * Adjusts height variation based on difficulty.
     */
    private void adjustHeightChances(@NotNull Profile profile, double difficulty) {
        double[] weights = DifficultyWeights.calculateHeightWeights(difficulty);

        profile.set("height.min", String.valueOf(MIN_HEIGHT));
        profile.set("height.max", String.valueOf(MAX_HEIGHT));

        int index = 0;
        for (int height = MIN_HEIGHT; height <= MAX_HEIGHT; height++) {
            int chance = (int) Math.round(weights[index] * 100);
            profile.set("height.chances." + height, String.valueOf(chance));
            index++;
        }
    }

    /**
     * Adjusts special block frequency based on difficulty.
     */
    private void adjustSpecialFrequency(@NotNull Profile profile, double difficulty) {
        double specialPercent = DifficultyWeights.calculateSpecialFrequency(difficulty);

        int defaultPercent = (int) Math.round(100.0 - specialPercent);
        int specialChance = (int) Math.round(specialPercent * 0.75);
        int schematicChance = (int) Math.round(specialPercent * 0.25);

        profile.set("type.default", String.valueOf(defaultPercent));
        profile.set("type.special", String.valueOf(specialChance));
        profile.set("type.schematic", String.valueOf(schematicChance));
    }

    /**
     * Calculates the effective difficulty of a profile.
     * Inverse operation of adjustProfile.
     *
     * @param profile The profile to analyze
     * @return Estimated difficulty (0.0-1.0)
     */
    public double estimateDifficulty(@NotNull Profile profile) {
        double distanceDifficulty = estimateDistanceDifficulty(profile);
        double heightDifficulty = estimateHeightDifficulty(profile);
        double specialDifficulty = estimateSpecialDifficulty(profile);

        // Weighted average
        return (distanceDifficulty * 0.5) + (heightDifficulty * 0.3) + (specialDifficulty * 0.2);
    }

    private double estimateDistanceDifficulty(@NotNull Profile profile) {
        double avgDistance = 0.0;
        double totalWeight = 0.0;

        for (int distance = MIN_DISTANCE; distance <= MAX_DISTANCE; distance++) {
            String key = "distance.chances." + distance;
            if (profile.settings.containsKey(key)) {
                double weight = profile.get(key).asDouble();
                avgDistance += distance * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight > 0) {
            avgDistance /= totalWeight;
        }

        // Map average distance to difficulty (1.0 = easy, 4.0 = hard)
        return (avgDistance - 1.0) / 3.0;
    }

    private double estimateHeightDifficulty(@NotNull Profile profile) {
        double variation = 0.0;
        double totalWeight = 0.0;

        for (int height = MIN_HEIGHT; height <= MAX_HEIGHT; height++) {
            String key = "height.chances." + height;
            if (profile.settings.containsKey(key)) {
                double weight = profile.get(key).asDouble();
                variation += Math.abs(height) * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight > 0) {
            variation /= totalWeight;
        }

        // Map variation to difficulty (0.0 = flat, 1.5 = varied)
        return Math.min(1.0, variation / 1.5);
    }

    private double estimateSpecialDifficulty(@NotNull Profile profile) {
        if (!profile.settings.containsKey("type.special")) {
            return 0.0;
        }

        double specialPercent = profile.get("type.special").asDouble();
        double minFrequency = 5.0;
        double maxFrequency = 30.0;

        // Map special percentage to difficulty
        return (specialPercent - minFrequency) / (maxFrequency - minFrequency);
    }
}
