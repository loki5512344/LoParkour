package dev.loki.loparkour.generator.lifecycle;

import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.generator.ParkourGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Manages tick cycle and periodic operations for generator lifecycle.
 */
public class LifecycleTickManager {
    
    private static final int TIME_UI_INTERVAL = 20; // 1 second — time display tick
    private static final int GENERATION_LEAD = 5;
    
    private final ParkourGenerator generator;
    private final GeneratorCleanup cleanup;
    private final LifecycleVisualUpdater visualUpdater;
    
    private int tickCounter = 0;
    
    public LifecycleTickManager(
            @NotNull ParkourGenerator generator,
            @NotNull GeneratorCleanup cleanup,
            @NotNull LifecycleVisualUpdater visualUpdater) {
        this.generator = generator;
        this.cleanup = cleanup;
        this.visualUpdater = visualUpdater;
    }
    
    /**
     * Start the tick cycle.
     */
    public void startTick() {
        if (generator.state.start == null) {
            generator.state.start = java.time.Instant.now();
        }

        // Generate initial blocks (reduced from GENERATION_LEAD to avoid too far generation)
        generator.generate(3);
    }
    
    /**
     * Main tick method called every server tick.
     */
    public void tick() {
        tickCounter++;

        // Check if players fell below threshold
        checkPlayerFall();

        if (tickCounter % Option.CLEANUP_INTERVAL == 0) {
            cleanup.cleanupDistantBlocks();
        }
        if (tickCounter % TIME_UI_INTERVAL == 0) {
            visualUpdater.updateTimeDisplays();
        }

        // Generate more blocks if needed
        maintainBlockLead();

        // Update player visuals
        visualUpdater.updateAllPlayers();
    }

    /**
     * Check if any player fell below the threshold.
     */
    private void checkPlayerFall() {
        if (generator.state.playerSpawn == null) {
            return;
        }

        double fallThreshold = generator.state.playerSpawn.getY() - 30;

        for (var player : generator.getPlayers()) {
            if (player.player.getLocation().getY() < fallThreshold) {
                generator.fall();
                break; // Only trigger once per tick
            }
        }
    }
    
    /**
     * Reset tick counter.
     */
    public void reset() {
        tickCounter = 0;
    }
    
    private void maintainBlockLead() {
        int currentBlocks = generator.state.history.size();
        int targetBlocks = generator.state.score + GENERATION_LEAD;
        
        if (currentBlocks < targetBlocks) {
            int needed = targetBlocks - currentBlocks;
            generator.generate(needed);
        }
    }
}
