package dev.loki.loparkour.generator.jump.placement;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourSchematicGenerateEvent;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.generator.jump.calculation.JumpCalculator;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.schematic.core.ParkourSchematic;
import dev.loki.loparkour.schematic.core.SchematicManager;
import dev.loki.loparkour.util.misc.Probs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    private boolean isTallMaterial(@NotNull Material mat) {
        return blockSelector.isFenceMaterial(mat) || blockSelector.isTrapdoorMaterial(mat) || mat == Material.GLASS_PANE;
    }

    /**
     * Select blocks with a forced height override for tall materials (fences, trapdoors).
     */
    @NotNull
    private List<Block> selectBlocks(int forcedHeight) {
        List<Block> blocks = new ArrayList<>();
        if (generator.state.history.isEmpty()) {
            return blocks;
        }
        Block current = generator.getLatest();
        int distance = Probs.random(generator.state.distanceChances);
        int height = forcedHeight != Integer.MIN_VALUE ? forcedHeight : Probs.random(generator.state.heightChances);
        Block nextBlock = jumpCalculator.calculateNextBlock(current, distance, height);
        blocks.add(nextBlock);
        return blocks;
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
        BlockData blockData = blockSelector.selectBlockData();
        if (blockData == null) {
            return;
        }

        Material mat = blockData.getMaterial();
        boolean tall = isTallMaterial(mat);

        List<Block> blocks = tall ? selectBlocks(-1) : selectBlocks();
        if (blocks.isEmpty()) {
            return;
        }

        Block selectedBlock = blocks.get(0);
        placeBlockData(selectedBlock, blockData);
        generator.state.history.add(selectedBlock);
    }

    private void placeSpecialBlock() {
        BlockData specialBlockData = Probs.random(generator.state.specialChances);
        if (specialBlockData == null) {
            placeNormalBlock();
            return;
        }

        Material mat = specialBlockData.getMaterial();
        boolean tall = isTallMaterial(mat);

        List<Block> blocks = tall ? selectBlocks(-1) : selectBlocks();
        if (blocks.isEmpty()) {
            return;
        }

        Block selectedBlock = blocks.get(0);
        placeBlockData(selectedBlock, specialBlockData);
        generator.state.history.add(selectedBlock);
    }

    /** Prefer no neighbor physics to avoid fragile vanilla multi-block checks in empty worlds. */
    private void placeBlockData(@NotNull Block block, @NotNull BlockData data) {
        block.setBlockData(data, false);

        // Play effects for all players
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        for (ParkourPlayer player : generator.getPlayers()) {
            // Spawn particles
            player.player.spawnParticle(Particle.BLOCK_CRACK, loc, 10, 0.3, 0.3, 0.3, 0.1, data);

            // Play sound
            player.player.playSound(loc, Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f);
        }
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
        SchematicManager manager = LoParkour.getSchematicManager();
        if (manager == null) {
            return false;
        }

        double maxDifficulty = generator.getPlayers().stream()
                .mapToDouble(p -> p.schematicDifficulty)
                .min()
                .orElse(1.0);

        ParkourSchematic schematic = manager.pick(maxDifficulty);
        if (schematic == null) {
            return false;
        }

        List<Block> blocks = selectBlocks();
        if (blocks.isEmpty()) {
            return false;
        }

        Location pasteLocation = blocks.get(0).getLocation();
        List<Block> schematicBlocks = schematic.paste(pasteLocation, pasteLocation.getWorld());
        if (schematicBlocks.isEmpty()) {
            return false;
        }

        generator.state.history.addAll(schematicBlocks);
        generator.state.schematicBlocks = schematicBlocks;

        for (ParkourPlayer player : generator.getPlayers()) {
            new ParkourSchematicGenerateEvent(schematic, generator, player).call();
        }
        return true;
    }
}
