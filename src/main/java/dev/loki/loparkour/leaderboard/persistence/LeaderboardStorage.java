package dev.loki.loparkour.leaderboard.persistence;
import dev.loki.loparkour.leaderboard.model.Score;
import dev.loki.loparkour.leaderboard.core.LeaderboardSorter;
import dev.loki.loparkour.leaderboard.core.Leaderboard;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.storage.Storage;
import dev.lolib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Handles I/O operations and periodic sync for leaderboard scores.
 */
public class LeaderboardStorage {

    private final String mode;
    private final Map<UUID, Score> scores;
    private final LeaderboardSorter sorter;

    public LeaderboardStorage(@NotNull String mode,
                              @NotNull Map<UUID, Score> scores,
                              @NotNull LeaderboardSorter sorter) {
        this.mode = mode.toLowerCase();
        this.scores = scores;
        this.sorter = sorter;
    }

    /**
     * Initialize storage and start periodic sync timer.
     */
    public void initAndSchedule() {
        Storage.init(mode);

        // Defer initial read until storage is ready (SQL connects async)
        Storage.runWhenReady(() -> read(true));

        var interval = Config.CONFIG.getInt("storage-update-interval");

        // Main-thread timer: only schedules I/O
        Scheduler.get(LoParkour.getPlugin()).runTimer(() -> {
            if (Config.CONFIG.getBoolean("joining")) {
                write(true);
            } else {
                read(true);
            }
        }, interval * 20, interval * 20);
    }

    /**
     * Write all scores to storage.
     */
    public void write(boolean async) {
        run(() -> Storage.writeScores(mode, scores), async);
    }

    /**
     * Read all scores from storage.
     */
    public void read(boolean async) {
        run(() -> {
            Map<UUID, Score> loadedScores = Storage.readScores(mode);

            // Synchronize access to prevent concurrent modification
            synchronized (scores) {
                scores.clear();
                scores.putAll(loadedScores);
            }

            sorter.sortInPlace(scores);
        }, async);
    }

    private void run(Runnable runnable, boolean async) {
        if (async) {
            Scheduler.get(LoParkour.getPlugin()).runAsync(runnable);
        } else {
            runnable.run();
        }
    }
}
