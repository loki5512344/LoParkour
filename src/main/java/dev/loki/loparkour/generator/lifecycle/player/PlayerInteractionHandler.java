package dev.loki.loparkour.generator.lifecycle.player;

import dev.loki.loparkour.adaptive.bootstrap.AdaptiveServices;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.generator.core.coordinator.GeneratorEventListener;
import dev.loki.loparkour.generator.lifecycle.loop.LifecycleEventManager;
import dev.loki.loparkour.player.core.ParkourPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

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

    /** Compare two blocks by their world coordinates (Bukkit Block uses reference equality). */
    private static boolean isSameBlock(@NotNull Block a, @NotNull Block b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    /** Check if history contains a block by coordinates, not reference. */
    private static boolean historyContains(@NotNull List<Block> history, @NotNull Block block) {
        for (Block b : history) {
            if (isSameBlock(b, block)) return true;
        }
        return false;
    }
    
    /**
     * Handle player scoring on a block.
     */
    public void handleScore() {
        // Start the timer on the very first scored block
        if (generator.state.start == null) {
            generator.state.start = Instant.now();
        }

        generator.state.score++;
        eventManager.handleScore();

        // Fire event listeners for adaptive system
        Block latestBlock = generator.getLatest();
        if (latestBlock != null) {
            for (ParkourPlayer player : generator.getPlayers()) {
                double distance = calculateDistance(player, latestBlock);
                for (GeneratorEventListener listener : generator.getEventListeners()) {
                    listener.onPlayerScore(player.player, latestBlock, distance);
                }
            }
        }

        // Block generation is handled by LifecycleTickManager.maintainBlockLead()
        // to avoid runaway generation in multiplayer (N players = N generates per tick otherwise).
    }
    
    /**
     * Handle player falling.
     */
    public void handleFall() {
        // Fire event listeners for adaptive system
        Block latestBlock = generator.getLatest();
        for (ParkourPlayer player : generator.getPlayers()) {
            for (GeneratorEventListener listener : generator.getEventListeners()) {
                listener.onPlayerFall(player.player, latestBlock);
            }
        }

        eventManager.handleFall();

        for (ParkourPlayer player : generator.getPlayers()) {
            AdaptiveServices.persistRatingAfterFall(player);
        }
        AdaptiveServices.refreshGeneratorDifficulty(generator);
    }
    
    private void handleBlockInteraction(@NotNull ParkourPlayer player, @NotNull Block block) {
        // Check if it's a schematic end block
        if (isSchematicEndBlock(block)) {
            handleSchematicEndBlock(block);
        } else if (isOnSchematicBlock(block)) {
            // Schematic block that is NOT the last — skip scoring
            return;
        }

        // Check for scoring (compare by coordinates, not reference — Bukkit Block uses identity)
        if (historyContains(generator.state.history, block) && !player.hasScored(block)) {
            player.markScored(block);
            handleScore();
        }
    }

    /** Check if block is any schematic block (not just the last one). */
    private boolean isOnSchematicBlock(@NotNull Block block) {
        return generator.state.schematicBlocks != null
                && historyContains(generator.state.schematicBlocks, block);
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
        List<Block> schematicBlocks = generator.state.schematicBlocks;
        if (schematicBlocks == null || schematicBlocks.isEmpty()) {
            return false;
        }
        // Only the LAST block of the schematic triggers completion
        Block last = schematicBlocks.get(schematicBlocks.size() - 1);
        return isSameBlock(block, last);
    }
    
    /** Block directly under feet; {@code null} in air — scoring runs when landed. */
    @Nullable
    private Block getBlockBelow(@NotNull ParkourPlayer player) {
        Location loc = player.player.getLocation().clone();
        Block below = loc.subtract(0, 1, 0).getBlock();

        return below.getType() != Material.AIR ? below : null;
    }

    /**
     * Calculate distance between player and block.
     */
    private double calculateDistance(@NotNull ParkourPlayer player, @NotNull Block block) {
        Location playerLoc = player.player.getLocation();
        Location blockLoc = block.getLocation().add(0.5, 0, 0.5);
        return playerLoc.distance(blockLoc);
    }
}
