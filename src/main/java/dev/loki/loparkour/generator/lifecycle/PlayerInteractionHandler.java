package dev.loki.loparkour.generator.lifecycle;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles player interactions with parkour blocks.
 */
public class PlayerInteractionHandler {
    
    private static final int GENERATION_LEAD = 5;
    
    private final ParkourGenerator generator;
    private final LifecycleEventManager eventManager;
    private final GeneratorCleanup cleanup;
    
    public PlayerInteractionHandler(
            @NotNull ParkourGenerator generator,
            @NotNull LifecycleEventManager eventManager,
            @NotNull GeneratorCleanup cleanup) {
        this.generator = generator;
        this.eventManager = eventManager;
        this.cleanup = cleanup;
    }
    
    /**
     * Check all players for block interactions.
     */
    public void checkAllPlayers() {
        for (ParkourPlayer player : generator.getPlayers()) {
            Block below = getBlockBelow(player);
            if (below != null) {
                handleBlockInteraction(player, below);
            }
        }
    }
    
    /**
     * Handle player scoring on a block.
     */
    public void handleScore() {
        generator.state.score++;
        eventManager.handleScore();
        
        // Generate more blocks after scoring
        generator.generate(1);
    }
    
    /**
     * Handle player falling.
     */
    public void handleFall() {
        eventManager.handleFall();
    }
    
    private void handleBlockInteraction(@NotNull ParkourPlayer player, @NotNull Block block) {
        // Check if it's a schematic end block
        if (isSchematicEndBlock(block)) {
            handleSchematicEndBlock(block);
        }
        
        // Check for scoring
        if (generator.state.history.contains(block) && !player.hasScored(block)) {
            player.markScored(block);
            handleScore();
        }
    }
    
    private void handleSchematicEndBlock(@NotNull Block block) {
        // Remove schematic blocks and continue with normal generation
        if (generator.state.schematicBlocks != null) {
            cleanup.removeTrailBlocks(0); // Remove schematic trail
            generator.state.schematicBlocks = null;
        }
        
        // Continue normal generation
        generator.generate(GENERATION_LEAD);
    }
    
    private boolean isSchematicEndBlock(@NotNull Block block) {
        LPSchematic schematic = generator.island.schematic;
        if (schematic == null) return false;
        
        // Simplified check - would need full schematic logic
        return generator.state.schematicBlocks != null && 
               generator.state.schematicBlocks.contains(block);
    }
    
    /** Block directly under feet; {@code null} in air — scoring runs when landed. */
    @Nullable
    private Block getBlockBelow(@NotNull ParkourPlayer player) {
        Location loc = player.player.getLocation().clone();
        Block below = loc.subtract(0, 1, 0).getBlock();

        return below.getType() != Material.AIR ? below : null;
    }
}
