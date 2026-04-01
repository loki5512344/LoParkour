package dev.loki.loparkour.generator.lifecycle;

import dev.loki.loparkour.generator.ParkourGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Refactored GeneratorLifecycle using composition pattern.
 * Coordinates tick management, player interactions, and visual updates.
 * 
 * Responsibilities delegated to:
 * - LifecycleTickManager: Tick cycle and periodic operations
 * - PlayerInteractionHandler: Block interactions and scoring
 * - LifecycleVisualUpdater: Visual updates for players
 * - LifecycleEventManager: Event firing and rewards
 * - GeneratorCleanup: Block cleanup operations
 */
public class GeneratorLifecycle {

    private final LifecycleTickManager tickManager;
    private final PlayerInteractionHandler interactionHandler;
    private final LifecycleVisualUpdater visualUpdater;
    private final LifecycleEventManager eventManager;
    private final GeneratorCleanup cleanup;
    
    public GeneratorLifecycle(@NotNull ParkourGenerator generator) {
        this.eventManager = new LifecycleEventManager(generator);
        this.cleanup = new GeneratorCleanup(generator);
        this.visualUpdater = new LifecycleVisualUpdater(generator);
        this.tickManager = new LifecycleTickManager(generator, cleanup, visualUpdater);
        this.interactionHandler = new PlayerInteractionHandler(generator, eventManager, cleanup);
    }
    
    /**
     * Start the tick cycle.
     */
    public void startTick() {
        tickManager.startTick();
    }
    
    /**
     * Main tick method called every server tick.
     */
    public void tick() {
        tickManager.tick();
        interactionHandler.checkAllPlayers();
    }
    
    /**
     * Handle player scoring.
     */
    public void score() {
        interactionHandler.handleScore();
    }
    
    /**
     * Handle player falling.
     */
    public void fall() {
        interactionHandler.handleFall();
    }
    
    /**
     * Reset the lifecycle state.
     */
    public void reset(boolean regenerate) {
        tickManager.reset();
        cleanup.reset(regenerate);
    }
}
