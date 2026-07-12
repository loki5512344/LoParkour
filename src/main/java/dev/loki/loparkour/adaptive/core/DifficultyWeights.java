package dev.loki.loparkour.adaptive.core;

/**
 * Calculates weight distributions for different difficulty levels.
 * Contains formulas for distance, height, and special block distributions.
 *
 * Thread-safe: Stateless, all methods are pure functions.
 */
class DifficultyWeights {

    private DifficultyWeights() {
    }

    /**
     * Calculates distance weight distribution for given difficulty.
     *
     * @param difficulty Target difficulty (0.0-1.0)
     * @return Array of weights [dist1, dist2, dist3, dist4]
     */
    static double[] calculateDistanceWeights(double difficulty) {
        double[] weights = new double[4];

        if (difficulty < 0.3) {
            // Easy: favor short jumps (1-2 blocks)
            weights[0] = 0.40; // 1 block
            weights[1] = 0.40; // 2 blocks
            weights[2] = 0.15; // 3 blocks
            weights[3] = 0.05; // 4 blocks
        } else if (difficulty < 0.6) {
            // Medium: balanced distribution
            weights[0] = 0.20; // 1 block
            weights[1] = 0.35; // 2 blocks
            weights[2] = 0.30; // 3 blocks
            weights[3] = 0.15; // 4 blocks
        } else if (difficulty < 0.8) {
            // Hard: favor longer jumps
            weights[0] = 0.10; // 1 block
            weights[1] = 0.25; // 2 blocks
            weights[2] = 0.40; // 3 blocks
            weights[3] = 0.25; // 4 blocks
        } else {
            // Expert: maximum challenge
            weights[0] = 0.05; // 1 block
            weights[1] = 0.15; // 2 blocks
            weights[2] = 0.35; // 3 blocks
            weights[3] = 0.45; // 4 blocks
        }

        return weights;
    }

    /**
     * Calculates height weight distribution for given difficulty.
     *
     * @param difficulty Target difficulty (0.0-1.0)
     * @return Array of weights [-1, 0, 1, 2]
     */
    static double[] calculateHeightWeights(double difficulty) {
        double[] weights = new double[4];

        if (difficulty < 0.3) {
            // Easy: mostly flat, minimal drops
            weights[0] = 0.10; // -1 (down)
            weights[1] = 0.60; // 0 (flat)
            weights[2] = 0.25; // +1 (up)
            weights[3] = 0.05; // +2 (high up)
        } else if (difficulty < 0.6) {
            // Medium: balanced variation
            weights[0] = 0.20; // -1
            weights[1] = 0.40; // 0
            weights[2] = 0.30; // +1
            weights[3] = 0.10; // +2
        } else if (difficulty < 0.8) {
            // Hard: more variation
            weights[0] = 0.25; // -1
            weights[1] = 0.30; // 0
            weights[2] = 0.30; // +1
            weights[3] = 0.15; // +2
        } else {
            // Expert: maximum variation
            weights[0] = 0.30; // -1
            weights[1] = 0.20; // 0
            weights[2] = 0.30; // +1
            weights[3] = 0.20; // +2
        }

        return weights;
    }

    /**
     * Calculates special block frequency for given difficulty.
     *
     * @param difficulty Target difficulty (0.0-1.0)
     * @return Special block percentage (5.0-30.0)
     */
    static double calculateSpecialFrequency(double difficulty) {
        double minFrequency = 5.0;
        double maxFrequency = 30.0;
        return minFrequency + (maxFrequency - minFrequency) * difficulty;
    }
}
