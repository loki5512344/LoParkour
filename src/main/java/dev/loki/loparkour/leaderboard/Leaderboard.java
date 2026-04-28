package dev.loki.loparkour.leaderboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Facade for leaderboard: CRUD operations with automatic sorting.
 * Delegates to LeaderboardStorage and LeaderboardSorter.
 */
public class Leaderboard {

    /**
     * The mode that this leaderboard belongs to
     */
    public final String mode;

    /**
     * The way in which items will be sorted
     */
    public final LeaderboardSorter.Sort sort;

    /**
     * A map of all scores for this mode (thread-safe)
     */
    public final Map<UUID, Score> scores = Collections.synchronizedMap(new LinkedHashMap<>());

    private final LeaderboardStorage storage;
    private final LeaderboardSorter sorter;

    public Leaderboard(@NotNull String mode, LeaderboardSorter.Sort sort) {
        this.mode = mode.toLowerCase();
        this.sort = sort;
        this.sorter = new LeaderboardSorter(sort);
        this.storage = new LeaderboardStorage(mode, scores, sorter);

        storage.initAndSchedule();
    }

    /**
     * @deprecated Use {@link #Leaderboard(String, LeaderboardSorter.Sort)} instead
     */
    @Deprecated
    public Leaderboard(@NotNull String mode, Sort sort) {
        this(mode, sort.toSorterSort());
    }

    // ── I/O operations ────────────────────────────────────────────────────────

    /**
     * Writes all scores to storage
     */
    public void write(boolean async) {
        storage.write(async);
    }

    /**
     * Reads all scores from storage
     */
    public void read(boolean async) {
        storage.read(async);
    }

    // ── CRUD operations ───────────────────────────────────────────────────────

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

        sorter.sortInPlace(scores);

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
     * @param uuid The {@link UUID} to get
     * @return The {@link Score} associated with the player. If null, returns a {@link Score} instance with "?"
     */
    @NotNull
    public Score get(@NotNull UUID uuid) {
        return scores.getOrDefault(uuid, new Score("?", "?", "?", 0));
    }

    /**
     * @param uuid The uuid
     * @return The rank. Starts from 1. Returns 0 if no ranking is found
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

    // ── Sorting ───────────────────────────────────────────────────────────────

    /**
     * Returns sorted copy of the score map.
     * @param sort The sorting method
     * @return A sorted map of scores
     */
    public Map<UUID, Score> sort(LeaderboardSorter.Sort sort) {
        synchronized (scores) {
            return sorter.sort(scores, sort);
        }
    }

    /**
     * @deprecated Use {@link #sort(LeaderboardSorter.Sort)} instead
     */
    @Deprecated
    public Map<UUID, Score> sort(Sort sort) {
        return sort(sort.toSorterSort());
    }

    // ── Backward compatibility ────────────────────────────────────────────────

    /**
     * @deprecated Use {@link LeaderboardSorter.Sort} instead
     */
    @Deprecated
    public enum Sort {
        SCORE, TIME, DIFFICULTY;

        public LeaderboardSorter.Sort toSorterSort() {
            return LeaderboardSorter.Sort.valueOf(this.name());
        }
    }
}
