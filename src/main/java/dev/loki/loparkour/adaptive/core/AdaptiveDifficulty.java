package dev.loki.loparkour.adaptive.core;

import dev.loki.loparkour.adaptive.model.AdaptiveConfig;
import dev.loki.loparkour.adaptive.model.SkillRating;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.generator.core.model.Profile;
import dev.loki.loparkour.player.core.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Applies adaptive difficulty to a generator {@link Profile}.
 */
public final class AdaptiveDifficulty {

    private static final DifficultyCalculator CALCULATOR = new DifficultyCalculator();
    private static final DifficultyAdjuster ADJUSTER = new DifficultyAdjuster();
    private static final SkillAnalyzer ANALYZER = new SkillAnalyzer();

    private static final Set<String> DIFFICULTY_PREFIXES = Set.of(
            "distance.", "height.", "type."
    );

    private AdaptiveDifficulty() {
    }

    /**
     * Merges difficulty-related keys from {@code adjusted} into {@code target}.
     */
    public static void mergeDifficultySettings(@NotNull Profile adjusted, @NotNull Profile target) {
        for (String key : adjusted.settings.keySet()) {
            if (isDifficultyKey(key)) {
                target.set(key, adjusted.get(key).value());
            }
        }
    }

    public static double resolveTargetDifficulty(
            @NotNull ParkourGenerator generator,
            @NotNull SkillRatingResolver resolver) {
        var players = generator.getPlayers();
        if (players.isEmpty()) {
            return 0.5;
        }

        double weightedSum = 0.0;
        double weightTotal = 0.0;

        for (ParkourPlayer player : players) {
            SkillRating rating = resolver.resolve(player);
            double target = CALCULATOR.calculateTargetDifficulty(rating);
            double weight = Math.max(0.1, rating.getConfidence());
            weightedSum += target * weight;
            weightTotal += weight;
        }

        return weightTotal > 0 ? weightedSum / weightTotal : 0.5;
    }

    @NotNull
    public static SkillRating mergeSessionRating(@NotNull SkillRating stored, @NotNull SkillRating sessionSample) {
        double weight = AdaptiveConfig.getRatingWeight();
        double merged = (stored.getRating() * (1.0 - weight)) + (sessionSample.getRating() * weight);
        double confidence = Math.max(stored.getConfidence(), sessionSample.getConfidence());
        return new SkillRating(stored.getPlayerUuid(), merged, confidence, stored.getSessionsCount() + 1);
    }

    @NotNull
    public static SkillRating analyzeSession(@NotNull ParkourPlayer player, @NotNull MetricsCollector collector) {
        return ANALYZER.analyzeSkill(collector.getMetrics(player.getUUID()));
    }

    public static void applyToProfile(
            @NotNull Profile baseProfile,
            double targetDifficulty) {
        Profile adjusted = ADJUSTER.adjustProfile(baseProfile, targetDifficulty);
        mergeDifficultySettings(adjusted, baseProfile);
    }

    private static boolean isDifficultyKey(@NotNull String key) {
        return DIFFICULTY_PREFIXES.stream().anyMatch(key::startsWith);
    }

    @FunctionalInterface
    public interface SkillRatingResolver {
        @NotNull
        SkillRating resolve(@NotNull ParkourPlayer player);
    }
}
