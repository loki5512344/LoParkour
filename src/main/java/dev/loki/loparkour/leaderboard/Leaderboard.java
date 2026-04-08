package dev.loki.loparkour.leaderboard;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.storage.Storage;
import dev.lolib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Class for handling leaderboards.
 */
public class Leaderboard {

    /**
     * The mode that this leaderboard belongs to
     */
    public final String mode;

    /**
     * The way in which items will be sorted.
     */
    public final Sort sort;

    /**
     * A map of all scores for this mode (thread-safe)
     */
    public final Map<UUID, Score> scores = Collections.synchronizedMap(new LinkedHashMap<>());

    public Leaderboard(@NotNull String mode, Sort sort) {
        this.mode = mode.toLowerCase();
        this.sort = sort;

        Storage.init(mode);

        // Defer initial read until storage is ready (SQL connects async — reading before
        // connection is established would return empty results and lose all scores).
        Storage.runWhenReady(() -> read(true));

        var interval = Config.CONFIG.getInt("storage-update-interval");

        // Main-thread timer: only schedules I/O; avoids async + Bukkit edge cases
        Scheduler.get(LoParkour.getPlugin()).runTimer(() -> {
            if (Config.CONFIG.getBoolean("joining")) {
                write(true);
            } else {
                read(true);
            }
        }, interval * 20, interval * 20);
    }

    /**
     * Writes all scores to the leaderboard file associated with this leaderboard
     */
    public void write(boolean async) {
        run(() -> Storage.writeScores(mode, scores), async);
    }

    /**
     * Reads all scores from the leaderboard file
     */
    public void read(boolean async) {
        run(() -> {
            Map<UUID, Score> loadedScores = Storage.readScores(mode);
            
            // Synchronize access to prevent concurrent modification
            synchronized (scores) {
                scores.clear();
                scores.putAll(loadedScores);
            }

            sort();
        }, async);
    }

    private void run(Runnable runnable, boolean async) {
        if (async) {
            Scheduler.get(LoParkour.getPlugin()).runAsync(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Returns sorted copy of the score map.
     * @param sort The sorting method.
     * @return A sorted map of scores.
     */
    public Map<UUID, Score> sort(Sort sort) {
        LinkedHashMap<UUID, Score> sorted = new LinkedHashMap<>();

        List<Map.Entry<UUID, Score>> snapshot;
        synchronized (scores) {
            snapshot = new ArrayList<>(scores.entrySet());
        }

        snapshot.stream()
                .sorted((one, two) -> {
                    switch (sort) {
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
                            return (int) Math.signum(Double.parseDouble(two.getValue().difficulty()) -
                                    Double.parseDouble(one.getValue().difficulty()));
                        }
                        default -> throw new IllegalArgumentException("Invalid sort method");
                    }
                })
                .forEachOrdered(entry -> sorted.put(entry.getKey(), entry.getValue()));

        return sorted;
    }

    // sorts all scores in the map
    private void sort() {
        var sorted = sort(sort);

        // Synchronize access to prevent concurrent modification
        synchronized (scores) {
            scores.clear();
            scores.putAll(sorted);
        }
    }

    /**
     * Registers a new score, overriding the old one
     *
     * @param uuid  The player's uuid
     * @param score The {@link Score} instance associated with a player's run
     * @return the previous score, if there was one
     */
    @Nullable
    public Score put(@NotNull UUID uuid, @NotNull Score score) {
        Score previous;
        synchronized (scores) {
            previous = scores.put(uuid, score);
        }

        sort();

        return previous;
    }

    /**
     * Resets the score of a player by deleting it from the internal map
     *
     * @param uuid The UUID
     * @return the previous value if one was found
     */
    @Nullable
    public Score remove(@NotNull UUID uuid) {
        return scores.remove(uuid);
    }

    /**
     * Resets all registered scores for this mode
     */
    public void resetAll() {
        List<UUID> keys;
        synchronized (scores) {
            keys = new ArrayList<>(scores.keySet());
        }
        keys.forEach(this::remove);
    }

    /**
     * @param uuid The {@link UUID} to get.
     * @return The {@link Score} associated with the player. If null, returns a {@link Score} instance with "?".
     */
    @NotNull
    public Score get(@NotNull UUID uuid) {
        return scores.getOrDefault(uuid, new Score("?", "?", "?", 0));
    }

    /**
     * @param uuid The uuid
     * @return The rank. Starts from 1. Returns 0 if no ranking is found.
     */
    public int getRank(@NotNull UUID uuid) {
        List<UUID> keys;
        synchronized (scores) {
            keys = new ArrayList<>(scores.keySet());
        }
        return keys.indexOf(uuid) + 1;
    }

    /**
     * Gets the score at a specified rank.
     * Ranks start at 1.
     *
     * @param rank The rank
     * @return the {@link Score} instance, null if one isn't found
     */
    @Nullable
    public Score getScoreAtRank(int rank) {
        List<Score> values;
        synchronized (scores) {
            if (scores.size() < rank) {
                return null;
            }
            values = new ArrayList<>(scores.values());
        }
        return values.get(rank - 1);
    }

    public enum Sort {
        SCORE, TIME, DIFFICULTY
    }
}
