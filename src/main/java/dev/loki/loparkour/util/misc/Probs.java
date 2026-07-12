package dev.loki.loparkour.util.misc;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Probability utilities (replacement for vilib Probs)
 */
public class Probs {

    private Probs() {
    }

    /**
     * Selects a random key from a map based on weighted probabilities
     *
     * @param chances Map of items to their probability weights
     * @param <T>     The type of items
     * @return A randomly selected item based on weights
     */
    public static <T> T random(Map<T, Double> chances) {
        return random(chances, ThreadLocalRandom.current());
    }

    /**
     * Selects a random key from a map based on weighted probabilities
     *
     * @param chances Map of items to their probability weights
     * @param random  Random instance to use
     * @param <T>     The type of items
     * @return A randomly selected item based on weights
     */
    public static <T> T random(Map<T, Double> chances, Random random) {
        double totalWeight = chances.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        
        double currentWeight = 0;
        for (Map.Entry<T, Double> entry : chances.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        // Fallback to first entry
        return chances.keySet().iterator().next();
    }

    /**
     * Normal probability density function
     *
     * @param mean   Mean value
     * @param stdDev Standard deviation
     * @param x      Value to calculate probability for
     * @return Probability density at x
     */
    public static double normalpdf(double mean, double stdDev, double x) {
        double exponent = -Math.pow(x - mean, 2) / (2 * Math.pow(stdDev, 2));
        return (1 / (stdDev * Math.sqrt(2 * Math.PI))) * Math.exp(exponent);
    }
}
