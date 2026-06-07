package dev.loki.loparkour.generator.jump.calculation;

import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.generator.core.model.GeneratorOption;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.generator.jump.placement.BlockSelector;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Calculates jump positions and validates jump constraints.
 */
public class JumpCalculator {
    
    private final ParkourGenerator generator;
    private final BlockSelector blockSelector;
    
    public JumpCalculator(@NotNull ParkourGenerator generator, @NotNull BlockSelector blockSelector) {
        this.generator = generator;
        this.blockSelector = blockSelector;
    }
    
    /**
     * Calculate the next block position based on current block and jump parameters.
     */
    @NotNull
    public Block calculateNextBlock(@NotNull Block current, int distance, int height) {
        // Ensure zone is initialized
        if (generator.state.zone == null || generator.state.zone.length < 2) {
            throw new IllegalStateException("Generator zone not initialized");
        }
        
        JumpDirector director = new JumpDirector(
            BoundingBox.of(generator.state.zone[0], generator.state.zone[1]),
            current.getLocation().toVector()
        );

        generator.state.heading = director.getRecommendedHeading(generator.state.heading);
        height = director.getRecommendedHeight(height);

        // Apply material-specific restrictions
        JumpConstraints constraints = calculateJumpConstraints(current);
        height = Math.min(height, constraints.maxHeight);
        distance = Math.min(distance, constraints.maxDistance);
        
        // Clamp values to valid ranges
        height = Math.max(-2, Math.min(2, height));
        distance = Math.max(1, Math.min(4, distance));

        if (height > 0) distance = Math.max(distance - height, 1);

        // Calculate offset
        Vector offset = calculateJumpOffset(distance, height);
        Block candidate = current.getLocation().add(offset).getBlock();
        
        // Validate jump with retry mechanism
        return validateAndRetryJump(current, candidate, distance, height);
    }
    
    /**
     * Calculate jump constraints based on the material of the current block.
     */
    @NotNull
    public JumpConstraints calculateJumpConstraints(@NotNull Block currentBlock) {
        Material material = currentBlock.getType();
        BlockData blockData = currentBlock.getBlockData();
        
        // Default constraints
        int maxHeight = 1;
        int maxDistance = 4;
        
        if (blockSelector.isSlabMaterial(material)) {
            if (blockData instanceof org.bukkit.block.data.type.Slab slab) {
                if (slab.getType() == org.bukkit.block.data.type.Slab.Type.BOTTOM) {
                    maxHeight = -1;  // Bottom slab: reduced height capability
                    maxDistance = 3;
                } else {
                    maxHeight = 0;   // Top slab: normal restrictions
                    maxDistance = 3;
                }
            } else {
                maxHeight = 0;
                maxDistance = 3;
            }
        } else if (material == Material.GLASS_PANE) {
            maxHeight = 0;
            maxDistance = 3;
        } else if (blockSelector.isSpecialMaterial(material)) {
            maxHeight = getSpecialMaterialMaxHeight(material);
            maxDistance = getSpecialMaterialMaxDistance(material);
        }
        
        return new JumpConstraints(maxHeight, maxDistance);
    }
    
    @NotNull
    private Vector calculateJumpOffset(int distance, int height) {
        double sd = java.util.Arrays.asList(generator.generatorOptions).contains(GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE) ? 0.5 : 1;
        int randomOffset = new JumpOffsetGenerator(height, distance).getRandomOffset(0, sd);

        Vector offset = generator.state.heading.clone().multiply(distance).setY(height);
        if (offset.getX() == 0) {
            offset.setX(randomOffset);
        } else {
            offset.setZ(randomOffset);
        }

        offset.rotateAroundY(angleInY(generator.state.heading, Option.HEADING.getDirection()));
        return offset;
    }
    
    @NotNull
    private Block validateAndRetryJump(@NotNull Block current, @NotNull Block candidate, int distance, int height) {
        JumpValidator validator = new JumpValidator();
        int attempts = 0;
        
        while (!validator.canJump(current.getLocation(), candidate.getLocation()) && attempts < 10) {
            // Reduce distance and height to make jump easier
            distance = Math.max(1, distance - 1);
            height = Math.max(-1, height - 1);
            
            Vector offset = calculateJumpOffset(distance, height);
            candidate = current.getLocation().add(offset).getBlock();
            attempts++;
        }
        
        return candidate;
    }
    
    private int getSpecialMaterialMaxHeight(@NotNull Material material) {
        return switch (material) {
            case PACKED_ICE -> 1;    // Ice allows normal height
            case BLUE_ICE -> 1;      // Blue ice same
            case ICE -> 0;           // Regular ice melts, slippery
            case LADDER -> 1;        // Ladder allows some height
            default -> 0;            // Most special materials restrict height
        };
    }

    private int getSpecialMaterialMaxDistance(@NotNull Material material) {
        return switch (material) {
            case PACKED_ICE -> 4;    // Ice — full distance, slippery
            case BLUE_ICE -> 4;      // Blue ice — full distance
            case ICE -> 3;           // Regular ice — reduced
            case LADDER -> 2;        // Ladder is very restrictive
            default -> 3;            // Default restriction
        };
    }
    
    private double angleInY(@NotNull Vector a, @NotNull Vector b) {
        return Math.atan2(b.getZ(), b.getX()) - Math.atan2(a.getZ(), a.getX());
    }
    
    /**
     * Represents jump constraints for a specific block type.
     */
    public static class JumpConstraints {
        public final int maxHeight;
        public final int maxDistance;
        
        public JumpConstraints(int maxHeight, int maxDistance) {
            this.maxHeight = maxHeight;
            this.maxDistance = maxDistance;
        }
    }
}
