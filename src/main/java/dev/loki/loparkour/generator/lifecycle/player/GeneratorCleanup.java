package dev.loki.loparkour.generator.lifecycle.player;

import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.player.core.ParkourPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles cleanup operations for parkour generator.
 */
public class GeneratorCleanup {

    private final ParkourGenerator generator;
    
    public GeneratorCleanup(@NotNull ParkourGenerator generator) {
        this.generator = generator;
    }
    
    /**
     * Remove only trail blocks <strong>behind</strong> players (oldest indices).
     * Distance-based cleanup was removing blocks ahead of the player and breaking scoring.
     */
    public void cleanupDistantBlocks() {
        List<Block> history = generator.state.history;
        if (history.isEmpty()) {
            return;
        }

        int minStandingIndex = Integer.MAX_VALUE;
        for (ParkourPlayer pp : generator.getPlayers()) {
            Block below = blockBelowPlayer(pp);
            if (below == null) {
                continue;
            }
            int idx = indexOfBlock(history, below);
            if (idx >= 0) {
                minStandingIndex = Math.min(minStandingIndex, idx);
            }
        }
        if (minStandingIndex == Integer.MAX_VALUE) {
            return;
        }

        int keepBehind = Math.max(0, Option.TRAIL_KEEP_BEHIND);
        int removeCount = minStandingIndex - keepBehind;
        if (removeCount <= 0) {
            return;
        }

        List<Block> head = new ArrayList<>(history.subList(0, removeCount));
        for (Block block : head) {
            if (block != null && block.getType() != Material.AIR) {
                block.setType(Material.AIR);
            }
        }
        history.subList(0, removeCount).clear();
    }

    /** Find index of a block in history by coordinates, not reference. */
    private static int indexOfBlock(@NotNull List<Block> list, @NotNull Block target) {
        for (int i = 0; i < list.size(); i++) {
            Block b = list.get(i);
            if (b != null && b.getX() == target.getX() && b.getY() == target.getY() && b.getZ() == target.getZ()) {
                return i;
            }
        }
        return -1;
    }

    private static Block blockBelowPlayer(@NotNull ParkourPlayer pp) {
        Location loc = pp.player.getLocation().clone();
        Block below = loc.subtract(0, 1, 0).getBlock();
        return below.getType() != Material.AIR ? below : null;
    }
    
    /**
     * Remove trail blocks behind player at specified index.
     */
    public void removeTrailBlocks(int startIndex) {
        if (startIndex < 0 || startIndex >= generator.state.history.size()) {
            return;
        }
        
        int endIndex = Math.min(startIndex + Option.BLOCK_CLEANUP_DISTANCE, generator.state.history.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Block block = generator.state.history.get(i);
            if (block != null && block.getType() != Material.AIR) {
                block.setType(Material.AIR);
            }
        }
        
        // Remove from history (synchronized to prevent concurrent index shift)
        synchronized (generator.state.history) {
            generator.state.history.subList(startIndex, endIndex).clear();
        }
    }
    
    /**
     * Reset generator state and clean up all blocks.
     */
    public void reset(boolean regenerate) {
        List<Block> snapshot = new ArrayList<>(generator.state.history);
        for (Block block : snapshot) {
            if (block != null) {
                block.setType(Material.AIR);
            }
        }
        
        // Clear schematic blocks
        if (generator.state.schematicBlocks != null) {
            for (Block block : generator.state.schematicBlocks) {
                if (block != null) {
                    block.setType(Material.AIR);
                }
            }
            generator.state.schematicBlocks.clear();
        }
        
        // Reset state
        generator.state.history.clear();
        generator.state.score = 0;
        generator.state.start = null;
        
        if (regenerate) {
            // Regenerate first block
            Location spawn = generator.state.playerSpawn;
            if (spawn != null) {
                // Place first block 6 blocks forward at same Y level as spawn
                Location blockSpawn = spawn.clone();

                // Move block forward based on heading (6 blocks forward, same height)
                switch (dev.loki.loparkour.config.options.Option.HEADING) {
                    case NORTH -> blockSpawn.add(0, 0, -6);
                    case SOUTH -> blockSpawn.add(0, 0, 6);
                    case WEST  -> blockSpawn.add(-6, 0, 0);
                    case EAST  -> blockSpawn.add(6, 0, 0);
                    default -> {}
                }

                generator.generateFirst(spawn, blockSpawn);
            }
        }
    }
    
}
