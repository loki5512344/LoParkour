package dev.loki.loparkour.generator;

import dev.loki.loparkour.config.Config;
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
     * Override profile settings based on generator options.
     */
    public void overrideProfile() {
        // Generator options are for disabling features, not difficulty
        // Difficulty is controlled by profile settings
        // This method can be used to apply custom profile overrides if needed
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
        
        // Distance difficulty
        for (Map.Entry<Integer, Double> entry : generator.state.distanceChances.entrySet()) {
            score += entry.getKey() * entry.getValue() * 0.3;
        }
        
        // Height difficulty  
        for (Map.Entry<Integer, Double> entry : generator.state.heightChances.entrySet()) {
            score += Math.abs(entry.getKey()) * entry.getValue() * 0.2;
        }
        
        // Special blocks difficulty
        for (Map.Entry<BlockData, Double> entry : generator.state.specialChances.entrySet()) {
            score += entry.getValue() * 0.5;
        }
        
        return Math.round(score * 100.0) / 100.0;
    }
    
    private void calculateDistanceChances() {
        generator.state.distanceChances.clear();
        
        // Get distance range from config
        int minDistance = Config.GENERATION.getInt("advanced.distance.min", 1);
        int maxDistance = Config.GENERATION.getInt("advanced.distance.max", 4);
        
        for (int distance = minDistance; distance <= maxDistance; distance++) {
            int chance = Config.GENERATION.getInt("advanced.distance.chances." + distance, 1);
            generator.state.distanceChances.put(distance, (double) chance);
        }
        
        // Normalize chances to sum to 1.0
        normalizeMap(generator.state.distanceChances);
    }
    
    private void calculateHeightChances() {
        generator.state.heightChances.clear();
        
        // Get height range from config
        int minHeight = Config.GENERATION.getInt("advanced.height.min", -1);
        int maxHeight = Config.GENERATION.getInt("advanced.height.max", 2);
        
        for (int height = minHeight; height <= maxHeight; height++) {
            int chance = Config.GENERATION.getInt("advanced.height.chances." + height, 1);
            generator.state.heightChances.put(height, (double) chance);
        }
        
        // Normalize chances to sum to 1.0
        normalizeMap(generator.state.heightChances);
    }
    
    private void calculateDefaultChances() {
        generator.state.defaultChances.clear();
        
        int defaultChance = Config.GENERATION.getInt("advanced.type.default", 80);
        int specialChance = Config.GENERATION.getInt("advanced.type.special", 15);
        int schematicChance = Config.GENERATION.getInt("advanced.type.schematic", 5);
        
        generator.state.defaultChances.put(ParkourGenerator.BlockGenerationType.DEFAULT, (double) defaultChance);
        generator.state.defaultChances.put(ParkourGenerator.BlockGenerationType.SPECIAL, (double) specialChance);
        generator.state.defaultChances.put(ParkourGenerator.BlockGenerationType.SCHEMATIC, (double) schematicChance);
        
        // Normalize chances to sum to 1.0
        normalizeMap(generator.state.defaultChances);
    }
    
    private void calculateSpecialChances() {
        generator.state.specialChances.clear();
        
        try {
            for (String specialType : Config.GENERATION.getChildren("advanced.special")) {
                String materialPath = "advanced.special." + specialType + ".material";
                String chancePath = "advanced.special." + specialType + ".chance";
                
                String materialName = Config.GENERATION.getString(materialPath);
                int chance = Config.GENERATION.getInt(chancePath, 1);
                
                BlockData blockData = org.bukkit.Bukkit.createBlockData(materialName);
                generator.state.specialChances.put(blockData, (double) chance);
            }
            
            // Normalize chances to sum to 1.0
            normalizeMap(generator.state.specialChances);
        } catch (Exception e) {
            // If special blocks config is missing, just skip
        }
    }
    
    /**
     * Normalize a map so all values sum to 1.0
     */
    private <K> void normalizeMap(Map<K, Double> map) {
        if (map.isEmpty()) return;
        
        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            map.replaceAll((k, v) -> v / sum);
        }
    }
}
