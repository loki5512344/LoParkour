package dev.loki.loparkour.generator.core.coordinator;

import dev.loki.loparkour.adaptive.bootstrap.AdaptiveServices;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.generator.core.model.Profile;
import dev.loki.loparkour.generator.profile.GenerationProfileLoader;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Manages generator profiles and difficulty calculations.
 */
public class GeneratorProfileManager {

    private final ParkourGenerator generator;

    public GeneratorProfileManager(@NotNull ParkourGenerator generator) {
        this.generator = generator;
    }

    /**
     * Rebuilds profile from config, player settings, and adaptive difficulty when enabled.
     */
    public void overrideProfile() {
        Profile rebuilt = GenerationProfileLoader.loadFromConfig();
        GenerationProfileLoader.preservePlayerSettings(generator.profile, rebuilt);
        AdaptiveServices.applyAdaptiveProfile(generator, rebuilt);
        GenerationProfileLoader.copyInto(generator.profile, rebuilt);
    }

    /**
     * Recalculates chances after a fall or settings change.
     */
    public void recalculateProfile() {
        overrideProfile();
        calculateChances();
    }

    /**
     * Calculate generation chances based on current profile.
     */
    public void calculateChances() {
        calculateDistanceChances();
        calculateHeightChances();
        calculateDefaultChances();
        calculateSpecialChances();
    }

    /**
     * Calculate difficulty score based on current settings.
     */
    public double getDifficultyScore() {
        double score = 0.0;

        for (Map.Entry<Integer, Double> entry : generator.state.distanceChances.entrySet()) {
            score += entry.getKey() * entry.getValue() * 0.3;
        }

        for (Map.Entry<Integer, Double> entry : generator.state.heightChances.entrySet()) {
            score += Math.abs(entry.getKey()) * entry.getValue() * 0.2;
        }

        for (Map.Entry<BlockData, Double> entry : generator.state.specialChances.entrySet()) {
            score += entry.getValue() * 0.5;
        }

        return Math.round(score * 100.0) / 100.0;
    }

    private void calculateDistanceChances() {
        generator.state.distanceChances.clear();

        int minDistance = profileInt("distance.min", "advanced.distance.min", 1);
        int maxDistance = profileInt("distance.max", "advanced.distance.max", 4);

        for (int distance = minDistance; distance <= maxDistance; distance++) {
            int chance = profileInt(
                    "distance.chances." + distance,
                    "advanced.distance.chances." + distance,
                    1);
            generator.state.distanceChances.put(distance, (double) chance);
        }

        normalizeMap(generator.state.distanceChances);
    }

    private void calculateHeightChances() {
        generator.state.heightChances.clear();

        int minHeight = profileInt("height.min", "advanced.height.min", -1);
        int maxHeight = profileInt("height.max", "advanced.height.max", 2);

        for (int height = minHeight; height <= maxHeight; height++) {
            int chance = profileInt(
                    "height.chances." + height,
                    "advanced.height.chances." + height,
                    1);
            generator.state.heightChances.put(height, (double) chance);
        }

        normalizeMap(generator.state.heightChances);
    }

    private void calculateDefaultChances() {
        generator.state.defaultChances.clear();

        int defaultChance = profileInt("type.default", "advanced.type.default", 80);
        int specialChance = profileInt("type.special", "advanced.type.special", 15);
        int schematicChance = profileInt("type.schematic", "advanced.type.schematic", 5);

        generator.state.defaultChances.put(ParkourGenerator.BlockGenerationType.DEFAULT, (double) defaultChance);
        generator.state.defaultChances.put(ParkourGenerator.BlockGenerationType.SPECIAL, (double) specialChance);
        generator.state.defaultChances.put(ParkourGenerator.BlockGenerationType.SCHEMATIC, (double) schematicChance);

        normalizeMap(generator.state.defaultChances);
    }

    private void calculateSpecialChances() {
        generator.state.specialChances.clear();

        try {
            double difficulty = getDifficultyScore();

            for (String specialType : Config.GENERATION.getChildren("advanced.special")) {
                String materialPath = "advanced.special." + specialType + ".material";
                String chancePath = "advanced.special." + specialType + ".chance";

                String materialName = Config.GENERATION.getString(materialPath);
                int chance = Config.GENERATION.getInt(chancePath, 1);

                BlockData blockData = org.bukkit.Bukkit.createBlockData(materialName);
                generator.state.specialChances.put(blockData, (double) chance);
            }

            // Add ice variants on higher difficulties
            // difficulty >= ~1.0 = hard: packed ice, >= ~2.0 = expert: blue ice
            if (difficulty >= 1.0) {
                generator.state.specialChances.put(
                    org.bukkit.Bukkit.createBlockData("minecraft:packed_ice"), 15.0);
            }
            if (difficulty >= 2.0) {
                generator.state.specialChances.put(
                    org.bukkit.Bukkit.createBlockData("minecraft:blue_ice"), 10.0);
            }

            normalizeMap(generator.state.specialChances);
        } catch (Exception ignored) {
            // special blocks config optional
        }
    }

    private int profileInt(@NotNull String profileKey, @NotNull String configKey, int defaultValue) {
        if (generator.profile.settings.containsKey(profileKey)) {
            return generator.profile.get(profileKey).asInt();
        }
        return Config.GENERATION.getInt(configKey, defaultValue);
    }

    private <K> void normalizeMap(Map<K, Double> map) {
        if (map.isEmpty()) {
            return;
        }

        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            map.replaceAll((k, v) -> v / sum);
        } else {
            double equalValue = 1.0 / map.size();
            map.replaceAll((k, v) -> equalValue);
        }
    }
}
