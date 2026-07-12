package dev.loki.loparkour.generator.core.coordinator;

import dev.loki.loparkour.generator.core.model.GeneratorOption;
import dev.loki.loparkour.generator.core.model.GeneratorState;
import dev.loki.loparkour.generator.core.model.Island;
import dev.loki.loparkour.generator.core.model.Profile;
import dev.loki.loparkour.generator.jump.placement.BlockPlacer;
import dev.loki.loparkour.generator.lifecycle.loop.GeneratorLifecycle;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.player.spectator.ParkourSpectator;
import dev.loki.loparkour.session.core.Session;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Refactored ParkourGenerator using composition pattern.
 * Coordinates profile management, statistics, and core generation.
 */
public class ParkourGenerator {
    
    public final Session session;
    public final Island island;
    public final Profile profile;
    public final GeneratorState state;
    public final GeneratorOption[] generatorOptions;
    
    protected final BlockPlacer placer;
    protected final GeneratorLifecycle lifecycle;
    public final GeneratorProfileManager profileManager;
    private final GeneratorStatistics statistics;

    // Event listeners for adaptive system integration
    private final List<GeneratorEventListener> eventListeners;
    
    public ParkourGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        this.session = session;
        this.generatorOptions = generatorOptions;
        this.island = new Island(session);
        this.profile = new Profile();
        this.state = new GeneratorState();
        this.eventListeners = new ArrayList<>();

        // Initialize zone from session
        if (session != null) {
            Location[] selection = dev.loki.loparkour.world.core.Divider.toSelection(session);
            if (selection == null || selection.length < 2) {
                throw new IllegalArgumentException("Invalid session zone: selection must contain at least 2 locations");
            }
            this.state.zone = selection;
        }

        // Initialize components
        this.placer = new BlockPlacer(this);
        this.lifecycle = new GeneratorLifecycle(this);
        this.profileManager = new GeneratorProfileManager(this);
        this.statistics = new GeneratorStatistics(this);

        // Setup initial state
        profileManager.overrideProfile();
        profileManager.calculateChances();
    }
    
    
    // Core generation methods
    public void generate() {
        placer.generate();
    }

    public void generate(int amount) {
        placer.generate(amount);
    }

    public void generateFirst(Location spawn, Location block) {
        placer.generateFirst(spawn, block);
    }

    // Lifecycle methods
    public void startTick() {
        lifecycle.startTick();
    }

    public void tick() {
        lifecycle.tick();
    }

    public void fall() {
        lifecycle.fall();
    }

    protected void score() {
        lifecycle.score();
    }
    
    public void reset(boolean regenerate) {
        lifecycle.reset(regenerate);
        
        if (regenerate) {
            profileManager.overrideProfile();
            profileManager.calculateChances();
        }
    }
    
    // Statistics and scoring
    public void registerScore(String time, String difficulty, int score) {
        statistics.registerScore(time, difficulty, score);
    }
    
    public double getDifficultyScore() {
        return profileManager.getDifficultyScore();
    }
    
    public String getFormattedTime() {
        return statistics.getFormattedTime();
    }
    
    public String getDetailedTime() {
        return statistics.getDetailedTime();
    }
    
    // UI and player management
    public void menu(ParkourPlayer player) { 
        Menus.PARKOUR_SETTINGS.open(player); 
    }
    
    public Mode getMode() { 
        return Modes.DEFAULT; 
    }
    
    @NotNull 
    public List<ParkourPlayer> getPlayers() { 
        return session.getPlayers(); 
    }
    
    @NotNull 
    public List<ParkourSpectator> getSpectators() { 
        return session.getSpectators(); 
    }
    
    /** Last generated block, or {@code null} if history is empty. */
    public @Nullable Block getLatest() {
        if (state.history.isEmpty()) {
            return null;
        }
        return state.history.get(state.history.size() - 1);
    }

    // Event listener management

    /**
     * Register an event listener for generator events.
     * Used by adaptive system to collect metrics without creating direct dependencies.
     *
     * @param listener The listener to register
     */
    public void registerEventListener(@NotNull GeneratorEventListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Unregister an event listener.
     *
     * @param listener The listener to unregister
     */
    public void unregisterEventListener(@NotNull GeneratorEventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Get all registered event listeners.
     *
     * @return Immutable list of event listeners
     */
    @NotNull
    public List<GeneratorEventListener> getEventListeners() {
        return new ArrayList<>(eventListeners);
    }

    public enum BlockGenerationType {
        DEFAULT, SPECIAL, SCHEMATIC
    }
}
