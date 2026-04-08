package dev.loki.loparkour.generator.jump;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import dev.loki.loparkour.util.Probs;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Refactored BlockPlacer using composition pattern.
 * Coordinates block selection, jump calculation, and placement.
 */
public class BlockPlacer {
    
    private final ParkourGenerator generator;
    private final BlockSelector blockSelector;
    private final JumpCalculator jumpCalculator;
    
    public BlockPlacer(@NotNull ParkourGenerator generator) {
        this.generator = generator;
        this.blockSelector = new BlockSelector(generator);
        this.jumpCalculator = new JumpCalculator(generator, blockSelector);
    }
    
    /**
     * Generate a single block.
     */
    public void generate() {
        generate(1);
    }
    
    /**
     * Generate specified amount of blocks.
     */
    public void generate(int amount) {
        for (int i = 0; i < amount; i++) {
            generateSingleBlock();
        }
    }
    
    /**
     * Generate the first block at specified locations.
     */
    public void generateFirst(@NotNull Location spawn, @NotNull Location blockSpawn) {
        Block block = blockSpawn.getBlock();
        BlockData blockData = blockSelector.selectBlockData();
        
        placeBlockData(block, blockData);
        generator.state.history.add(block);
        generator.state.playerSpawn = spawn;
    }
    
    /**
     * Delete schematic blocks (if any).
     */
    public void deleteSchematic() {
        if (generator.state.schematicBlocks != null) {
            generator.state.schematicBlocks.forEach(block -> block.setType(org.bukkit.Material.AIR));
            generator.state.schematicBlocks.clear();
        }
    }
    
    private void generateSingleBlock() {
        ParkourGenerator.BlockGenerationType jumpType = selectGenerationType();
        
        switch (jumpType) {
            case DEFAULT -> placeNormalBlock();
            case SPECIAL -> placeSpecialBlock();
            case SCHEMATIC -> {
                if (!tryGenerateSchematic()) {
                    placeNormalBlock(); // Fallback to normal block
                }
            }
        }
    }
    
    @NotNull
    private ParkourGenerator.BlockGenerationType selectGenerationType() {
        return Probs.random(generator.state.defaultChances);
    }
    
    private void placeNormalBlock() {
        List<Block> blocks = selectBlocks();
        if (blocks.isEmpty()) return;

        Block selectedBlock = blocks.get(0); // Use first block for simplicity
        BlockData blockData = blockSelector.selectBlockData();
        if (blockData == null) {
            return; // Skip if no valid block data available
        }

        placeBlockData(selectedBlock, blockData);
        generator.state.history.add(selectedBlock);
    }
    
    private void placeSpecialBlock() {
        // Select special block type based on probabilities
        BlockData specialBlockData = Probs.random(generator.state.specialChances);
        if (specialBlockData == null) {
            placeNormalBlock();
            return;
        }
        
        List<Block> blocks = selectBlocks();
        if (blocks.isEmpty()) return;
        
        Block selectedBlock = blocks.get(0);
        placeBlockData(selectedBlock, specialBlockData);
        generator.state.history.add(selectedBlock);
    }

    /** Prefer no neighbor physics to avoid fragile vanilla multi-block checks in empty worlds. */
    private static void placeBlockData(@NotNull Block block, @NotNull BlockData data) {
        block.setBlockData(data, false);
    }
    
    @NotNull
    private List<Block> selectBlocks() {
        List<Block> blocks = new ArrayList<>();
        
        if (generator.state.history.isEmpty()) {
            return blocks; // No previous blocks to work with
        }
        
        Block current = generator.getLatest();
        
        // Select jump parameters
        int distance = Probs.random(generator.state.distanceChances);
        int height = Probs.random(generator.state.heightChances);
        
        // Calculate next block position
        Block nextBlock = jumpCalculator.calculateNextBlock(current, distance, height);
        blocks.add(nextBlock);
        
        return blocks;
    }
    
    private boolean tryGenerateSchematic() {
        // Simplified schematic generation
        LPSchematic schematic = generator.island.schematic; // Получаем схематик из island
        if (schematic == null) return false;
        
        List<Block> blocks = selectBlocks();
        if (blocks.isEmpty()) return false;
        
        Block targetBlock = blocks.get(0);
        Location pasteLocation = targetBlock.getLocation();
        
        // Paste schematic (simplified)
        List<Block> schematicBlocks = pasteSchematic(schematic, pasteLocation);
        
        if (!schematicBlocks.isEmpty()) {
            generator.state.history.addAll(schematicBlocks);
            generator.state.schematicBlocks = schematicBlocks;
            return true;
        }
        
        return false;
    }
    
    @NotNull
    private List<Block> pasteSchematic(@NotNull LPSchematic schematic, @NotNull Location location) {
        // Use the schematic's built-in paste method
        return schematic.paste(location, location.getWorld());
    }
}