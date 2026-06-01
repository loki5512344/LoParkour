package dev.loki.loparkour.adaptive.core;

import dev.loki.loparkour.adaptive.model.PlayerMetrics;
import dev.loki.loparkour.adaptive.storage.StatsRepository;
import dev.loki.loparkour.generator.core.coordinator.GeneratorEventListener;
import dev.loki.loparkour.generator.jump.calculation.JumpType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects and manages player performance metrics.
 *
 * Responsibilities:
 * - Track player jumps, falls, and scoring events
 * - Detect near-miss jumps (distance > 3.5 blocks)
 * - Calculate average time per block
 * - Maintain jump type statistics
 * - Auto-save metrics every 5 minutes
 *
 * Thread-safe: Uses ConcurrentHashMap for player metrics cache.
 */
public class MetricsCollector implements GeneratorEventListener {

    private static final double NEAR_MISS_THRESHOLD = 3.5;
    private static final long AUTO_SAVE_INTERVAL_TICKS = 20 * 60 * 5; // 5 minutes

    private final Plugin plugin;
    private final StatsRepository repository;
    private final Map<UUID, PlayerMetrics> metricsCache;
    private final Map<UUID, Block> lastBlockCache;
    private final Map<UUID, Long> lastBlockTimeCache;

    private BukkitRunnable autoSaveTask;

    /**
     * Create a new MetricsCollector.
     *
     * @param plugin The plugin instance for scheduling tasks
     * @param repository The repository for persisting metrics
     */
    public MetricsCollector(@NotNull Plugin plugin, @NotNull StatsRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.metricsCache = new ConcurrentHashMap<>();
        this.lastBlockCache = new ConcurrentHashMap<>();
        this.lastBlockTimeCache = new ConcurrentHashMap<>();
    }

    /**
     * Start the auto-save task.
     * Should be called when the collector is initialized.
     */
    public void startAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveAllMetrics();
            }
        };

        autoSaveTask.runTaskTimerAsynchronously(plugin, AUTO_SAVE_INTERVAL_TICKS, AUTO_SAVE_INTERVAL_TICKS);
    }

    /**
     * Stop the auto-save task.
     * Should be called when the plugin is disabled.
     */
    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    /**
     * Get or create metrics for a player.
     *
     * @param playerUuid The player's UUID
     * @return The player's metrics
     */
    @NotNull
    public PlayerMetrics getMetrics(@NotNull UUID playerUuid) {
        return metricsCache.computeIfAbsent(playerUuid, uuid -> {
            // Try to load from repository
            PlayerMetrics loaded = repository.loadMetrics(uuid);
            if (loaded != null) {
                return loaded;
            }
            // Create new metrics if not found
            return new PlayerMetrics(uuid);
        });
    }

    /**
     * Called when a player lands on a block.
     * Detects near-miss jumps and updates timing metrics.
     *
     * @param player The player who landed
     * @param block The block the player landed on
     * @param distance The distance of the jump
     */
    public void onBlockLand(@NotNull Player player, @NotNull Block block, double distance) {
        UUID uuid = player.getUniqueId();
        PlayerMetrics metrics = getMetrics(uuid);

        // Calculate time since last block
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastBlockTimeCache.get(uuid);
        if (lastTime != null) {
            long timeDiff = currentTime - lastTime;
            // Record jump with timing (use "normal" as default type)
            metrics.recordJump("normal", timeDiff);
        }
        lastBlockTimeCache.put(uuid, currentTime);

        // Detect near-miss (long jump that succeeded)
        if (distance > NEAR_MISS_THRESHOLD) {
            metrics.recordNearMiss();
        }

        // Update last block
        lastBlockCache.put(uuid, block);
    }

    @Override
    public void onBlockGenerated(@NotNull Player player, @NotNull Block block, @NotNull JumpType jumpType, double distance) {
        // Not used for metrics collection, but required by interface
    }

    @Override
    public void onPlayerScore(@NotNull Player player, @NotNull Block block, double distance) {
        onBlockLand(player, block, distance);
    }

    @Override
    public void onPlayerFall(@NotNull Player player, @Nullable Block lastBlock) {
        UUID uuid = player.getUniqueId();
        PlayerMetrics metrics = getMetrics(uuid);

        // Save metrics on fall (important event)
        repository.saveMetrics(metrics);
    }

    @Override
    public void onJump(@NotNull Player player, @NotNull JumpType jumpType) {
        UUID uuid = player.getUniqueId();
        PlayerMetrics metrics = getMetrics(uuid);

        // Calculate time since last block
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastBlockTimeCache.get(uuid);
        long timeDiff = (lastTime != null) ? (currentTime - lastTime) : 0;

        // Record jump with type and timing
        metrics.recordJump(jumpType.name().toLowerCase(), timeDiff);
    }

    /**
     * Save metrics for a specific player.
     *
     * @param playerUuid The player's UUID
     */
    public void saveMetrics(@NotNull UUID playerUuid) {
        PlayerMetrics metrics = metricsCache.get(playerUuid);
        if (metrics != null) {
            repository.saveMetrics(metrics);
        }
    }

    /**
     * Save all cached metrics.
     * Called by auto-save task and on plugin disable.
     */
    public void saveAllMetrics() {
        for (PlayerMetrics metrics : metricsCache.values()) {
            repository.saveMetrics(metrics);
        }
    }

    /**
     * Remove a player from the cache.
     * Should be called when a player leaves the server.
     *
     * @param playerUuid The player's UUID
     */
    public void unloadPlayer(@NotNull UUID playerUuid) {
        // Save before removing
        saveMetrics(playerUuid);
        metricsCache.remove(playerUuid);
        lastBlockCache.remove(playerUuid);
        lastBlockTimeCache.remove(playerUuid);
    }

    /**
     * Get the number of cached players.
     *
     * @return The cache size
     */
    public int getCacheSize() {
        return metricsCache.size();
    }
}
