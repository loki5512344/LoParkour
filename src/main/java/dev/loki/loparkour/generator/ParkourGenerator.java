package dev.loki.loparkour.generator;

import dev.loki.loparkour.generator.jump.BlockPlacer;
import dev.loki.loparkour.generator.lifecycle.GeneratorLifecycle;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import dev.loki.loparkour.session.Session;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    
    public ParkourGenerator(@NotNull Session session, @Nullable LPSchematic schematic, GeneratorOption... generatorOptions) {
        this.session = session;
        this.generatorOptions = generatorOptions;
        this.island = new Island(session, schematic);
        this.profile = new Profile();
        this.state = new GeneratorState();
        
        // Initialize zone from session
        if (session != null) {
            Location[] selection = dev.loki.loparkour.world.Divider.toSelection(session);
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
    
    public ParkourGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        this(session, null, generatorOptions);
    }
    
    // Core generation methods
    public void generate() { placer.generate(); }
    public void generate(int amount) { placer.generate(amount); }
    public void generateFirst(Location spawn, Location block) { placer.generateFirst(spawn, block); }
    
    // Lifecycle methods
    public void startTick() { lifecycle.startTick(); }
    public void tick() { lifecycle.tick(); }
    protected void fall() { lifecycle.fall(); }
    protected void score() { lifecycle.score(); }
    
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
        if (state.history.isEmpty()) return null;
        return state.history.get(state.history.size() - 1);
    }
    
    public enum BlockGenerationType {
        DEFAULT, SPECIAL, SCHEMATIC
    }
}