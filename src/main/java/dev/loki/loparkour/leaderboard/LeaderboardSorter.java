package dev.loki.loparkour.leaderboard;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Sorting logic for leaderboard scores.
 */
public class LeaderboardSorter {

    private final Sort sort;

    public LeaderboardSorter(@NotNull Sort sort) {
        this.sort = sort;
    }

    /**
     * Returns sorted copy of the score map.
     * @param scores The scores to sort
     * @return A sorted map of scores
     */
    @NotNull
    public Map<UUID, Score> sort(@NotNull Map<UUID, Score> scores) {
        return sort(scores, this.sort);
    }

    /**
     * Returns sorted copy of the score map with custom sort method.
     * @param scores The scores to sort
     * @param sortMethod The sorting method
     * @return A sorted map of scores
     */
    @NotNull
    public Map<UUID, Score> sort(@NotNull Map<UUID, Score> scores, @NotNull Sort sortMethod) {
        LinkedHashMap<UUID, Score> sorted = new LinkedHashMap<>();

        List<Map.Entry<UUID, Score>> snapshot = new ArrayList<>(scores.entrySet());

        snapshot.stream()
                .sorted((one, two) -> {
                    switch (sortMethod) {
                        case SCORE -> {
                            int scoreComparison = two.getValue().score() - one.getValue().score();

                            if (scoreComparison != 0) {
                                return scoreComparison;
                            } else {
                                // Use Integer.compare to avoid overflow
                                return Integer.compare(one.getValue().getTimeMillis(), two.getValue().getTimeMillis());
                            }
                        }
                        case TIME -> {
                            // Use Integer.compare to avoid overflow
                            return Integer.compare(one.getValue().getTimeMillis(), two.getValue().getTimeMillis());
                        }
                        case DIFFICULTY -> {
                            String diff1 = one.getValue().difficulty();
                            String diff2 = two.getValue().difficulty();

                            // Handle "?" as lowest difficulty
                            if ("?".equals(diff1) && "?".equals(diff2)) return 0;
                            if ("?".equals(diff1)) return 1;
                            if ("?".equals(diff2)) return -1;

                            try {
                                return (int) Math.signum(Double.parseDouble(diff2) - Double.parseDouble(diff1));
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        }
                        default -> throw new IllegalArgumentException("Invalid sort method");
                    }
                })
                .forEachOrdered(entry -> sorted.put(entry.getKey(), entry.getValue()));

        return sorted;
    }

    /**
     * Sort the map in-place (modifies the original map).
     */
    public void sortInPlace(@NotNull Map<UUID, Score> scores) {
        Map<UUID, Score> sorted = sort(scores);
        scores.clear();
        scores.putAll(sorted);
    }

    public enum Sort {
        SCORE, TIME, DIFFICULTY
    }
}
