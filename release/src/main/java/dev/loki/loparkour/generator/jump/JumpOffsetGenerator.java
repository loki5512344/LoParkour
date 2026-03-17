package dev.loki.loparkour.generator.jump;

import dev.loki.loparkour.util.Probs;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Generates a random offset (the sidestep) for a parkour block.
 * Source: <a href="https://i.imgur.com/RPevMfX.png">here</a>.
 */
public class JumpOffsetGenerator {

    private final int maxOffset;

    /**
     * Constructor.
     *
     * @param y The height difference.
     */
    public JumpOffsetGenerator(int y, int distance) {
        this.maxOffset = switch (y) {
            case 1 -> switch (distance) {
                case 1 -> 4;
                case 2 -> 3;
                default -> 2;
            };
            case 0 -> switch (distance) {
                case 1, 2 -> 4;
                case 3 -> 3;
                default -> 2;
            };
            case -1 -> switch (distance) {
                case 1, 2 -> 5;
                case 3 -> 4;
                default -> 3;
            };
            case -2 -> switch (distance) {
                case 1, 2 -> 5;
                default -> 4;
            };
            default -> {
                // Fallback for unexpected values
                dev.loki.loparkour.LoParkour.getPlugin().getLogger()
                    .warning("Unexpected jump height " + y + ", using default maxOffset=2");
                yield 2;
            }
        };
    }

    /**
     * @param mean              The mean (average) value for the offset. Usually 0 to avoid parkour going only left or right.
     * @param standardDeviation The standard deviation.
     * @param random            The random instance.
     * @return A random jump-able offset.
     */
    public int getRandomOffset(double mean, double standardDeviation, Random random) {
        Map<Double, Double> distribution = java.util.stream.IntStream.range(-maxOffset, maxOffset + 1)
                .boxed()
                .collect(Collectors.toMap(
                    offset -> (double) offset, 
                    offset -> Probs.normalpdf(mean, standardDeviation, (double) offset)
                ));

        return (int) (double) Probs.random(distribution, random);
    }

    /**
     * @param mean              The mean (average) value for the offset. Usually 0 to avoid parkour going only left or right.
     * @param standardDeviation The standard deviation.
     * @return A random jump-able offset.
     */
    public int getRandomOffset(double mean, double standardDeviation) {
        return getRandomOffset(mean, standardDeviation, ThreadLocalRandom.current());
    }
}
