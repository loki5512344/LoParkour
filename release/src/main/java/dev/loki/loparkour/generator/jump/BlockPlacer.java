package dev.loki.loparkour.generator.jump;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourBlockGenerateEvent;
import dev.loki.loparkour.api.event.ParkourSchematicGenerateEvent;
import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.generator.GeneratorOption;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.style.Style;
import dev.loki.loparkour.util.Probs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Handles block selection, placement, and schematic generation.
 * Extracted from {@link ParkourGenerator}.
 */
public class BlockPlacer {

    private final ParkourGenerator g;

    public BlockPlacer(ParkourGenerator generator) {
        this.g = generator;
    }

    // ── Public entry points ────────────────────────────────────────────────────

    /** Generates one block or schematic ahead. */
    public void generate() {
        if (g.state.waitForSchematicCompletion) return;

        Map<ParkourGenerator.BlockGenerationType, Double> chances = new HashMap<>(g.state.defaultChances);
        if (g.state.schematicCooldown > 0
                || g.generatorOptions.contains(GeneratorOption.DISABLE_SCHEMATICS)
                || g.profile.get("schematicDifficulty").asDouble() == 0.0
                || !g.state.schematicBlocks.isEmpty()) {
            chances.remove(ParkourGenerator.BlockGenerationType.SCHEMATIC);
        }
        if (!g.profile.get("useSpecialBlocks").asBoolean()) {
            chances.remove(ParkourGenerator.BlockGenerationType.SPECIAL);
        }
        if (chances.isEmpty()) chances.put(ParkourGenerator.BlockGenerationType.DEFAULT, 1.0);

        ParkourGenerator.BlockGenerationType jump = Probs.random(chances);
        if (jump == ParkourGenerator.BlockGenerationType.SCHEMATIC && tryGenerateSchematic()) return;

        placeNormalBlock(jump);
    }

    /** Generates {@code amount+1} blocks. */
    public void generate(int amount) {
        for (int i = 0; i <= amount; i++) generate();
    }

    /** Seeds the history and generates the initial lead. */
    public void generateFirst(Location spawn, Location blockSpawn) {
        g.state.playerSpawn = spawn;
        g.state.lastStandingPlayerLocation = spawn;
        g.state.blockSpawn = blockSpawn;
        g.state.history.add(blockSpawn.getBlock());
        generate(g.profile.get("blockLead").asInt());
    }

    /** Clears schematic blocks and resets cooldown. */
    public void deleteSchematic() {
        if (!g.state.deleteSchematic) return;
        g.state.schematicBlocks.forEach(b -> b.setType(Material.AIR));
        g.state.schematicBlocks.clear();
        g.state.deleteSchematic = false;
        g.state.schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
    }

    // ── Block selection ────────────────────────────────────────────────────────

    protected List<Block> selectBlocks() {
        int height   = Probs.random(g.state.heightChances);
        int distance = Probs.random(g.state.distanceChances);
        return List.of(selectNext(g.getLatest(), distance, height));
    }

    protected Block selectNext(Block current, int distance, int height) {
        JumpDirector director = new JumpDirector(
            BoundingBox.of(g.zone[0], g.zone[1]), g.getLatest().getLocation().toVector());

        g.state.heading = director.getRecommendedHeading(g.state.heading);
        height    = director.getRecommendedHeight(height);

        // Restrict jumps after special blocks to prevent impossible jumps
        Material lastType = g.getLatest().getType();
        switch (lastType) {
            case SMOOTH_QUARTZ_SLAB -> {
                height = Math.min(height, 0);  // Can't jump up from slab
                distance = Math.min(distance, 3);  // Limit distance
            }
            case GLASS_PANE -> {
                height = Math.min(height, 0);  // Can't jump up from pane
                distance = Math.min(distance, 3);  // Limit distance
            }
            case OAK_FENCE, BIRCH_FENCE, SPRUCE_FENCE, DARK_OAK_FENCE, 
                 JUNGLE_FENCE, ACACIA_FENCE, MANGROVE_FENCE, CHERRY_FENCE,
                 CRIMSON_FENCE, WARPED_FENCE, NETHER_BRICK_FENCE -> {
                height = Math.min(height, 0);  // Can't jump up from fence
                distance = Math.min(distance, 3);  // Limit distance
            }
            case PACKED_ICE -> {
                distance = Math.min(distance, 3);  // Ice is slippery, limit distance
            }
            case OAK_TRAPDOOR, BIRCH_TRAPDOOR, SPRUCE_TRAPDOOR, DARK_OAK_TRAPDOOR,
                 JUNGLE_TRAPDOOR, ACACIA_TRAPDOOR, MANGROVE_TRAPDOOR, CHERRY_TRAPDOOR,
                 CRIMSON_TRAPDOOR, WARPED_TRAPDOOR, IRON_TRAPDOOR -> {
                height = Math.min(height, 0);  // Can't jump up from trapdoor
                distance = Math.min(distance, 2);  // Very limited distance
            }
            case LADDER -> {
                height = Math.min(height, 1);  // Limited height from ladder
                distance = Math.min(distance, 2);  // Very limited distance
            }
        }
        
        // Clamp values to valid ranges for JumpOffsetGenerator
        height = Math.max(-2, Math.min(1, height));
        distance = Math.max(1, Math.min(4, distance));
        
        if (height > 0) distance = Math.max(distance - height, 1);

        double sd = g.generatorOptions.contains(GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE) ? 0.5 : 1;
        int randomOffset = new JumpOffsetGenerator(height, distance).getRandomOffset(0, sd);

        Vector offset = g.state.heading.clone().multiply(distance + 1).setY(height);
        if (offset.getX() == 0) offset.setX(randomOffset);
        else                    offset.setZ(randomOffset);

        offset.rotateAroundY(angleInY(g.state.heading, Option.HEADING.getDirection()));
        
        Block candidate = current.getLocation().add(offset).getBlock();
        
        // Retry with JumpValidator if jump is impossible (up to 10 attempts)
        JumpValidator validator = new JumpValidator();
        int attempts = 0;
        while (!validator.canJump(current.getLocation(), candidate.getLocation()) && attempts < 10) {
            // Reduce distance and height to make jump easier
            distance = Math.max(1, distance - 1);
            height = Math.max(-1, height - 1);
            
            randomOffset = new JumpOffsetGenerator(height, distance).getRandomOffset(0, sd);
            offset = g.state.heading.clone().multiply(distance + 1).setY(height);
            if (offset.getX() == 0) offset.setX(randomOffset);
            else                    offset.setZ(randomOffset);
            offset.rotateAroundY(angleInY(g.state.heading, Option.HEADING.getDirection()));
            
            candidate = current.getLocation().add(offset).getBlock();
            attempts++;
        }
        
        return candidate;
    }

    protected BlockData selectBlockData() {
        Style style = Registry.getStyle(g.profile.get("style").value());
        if (style == null) {
            g.profile.set("style", Registry.getStyles().stream().findFirst().orElseThrow().getName());
            return selectBlockData();
        }
        return style.getNext().createBlockData();
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private void placeNormalBlock(ParkourGenerator.BlockGenerationType jump) {
        List<Block> blocks = selectBlocks();
        if (blocks.isEmpty()) {
            LoParkour.getPlugin().getLogger().severe("No blocks to generate!");
            return;
        }

        List<Block> placed = new ArrayList<>();
        for (Block block : blocks) {
            BlockData data;
            
            // Select JumpType based on chances (70% normal, 30% special types)
            JumpType jumpType = selectJumpType();
            
            if (jumpType != JumpType.NORMAL && !g.generatorOptions.contains(GeneratorOption.DISABLE_SPECIAL)) {
                // Use JumpType material
                data = jumpType.getRandomMaterial().createBlockData();
            } else if (jump == ParkourGenerator.BlockGenerationType.SPECIAL
                    && !g.generatorOptions.contains(GeneratorOption.DISABLE_SPECIAL)) {
                data = Probs.random(g.state.specialChances);
            } else {
                data = selectBlockData();
            }

            if (data instanceof Fence) block = block.getLocation().subtract(0, 1, 0).getBlock();
            block.setBlockData(data, data instanceof Fence || data instanceof GlassPane);
            placed.add(block);
        }

        new ParkourBlockGenerateEvent(placed, g, g.player).call();
        g.effects.particles(placed);
        g.effects.sound(placed);
        g.state.history.addAll(placed);
        g.state.schematicCooldown--;
    }
    
    private JumpType selectJumpType() {
        // Simple chances: 70% normal, 5% each for special types
        Map<JumpType, Double> chances = new HashMap<>();
        chances.put(JumpType.NORMAL, 70.0);
        chances.put(JumpType.NEO_JUMP, 5.0);
        chances.put(JumpType.HEAD_HITTER, 5.0);
        chances.put(JumpType.FENCE_JUMP, 10.0);
        chances.put(JumpType.TRAPDOOR_JUMP, 5.0);
        chances.put(JumpType.LADDER_JUMP, 5.0);
        
        return Probs.random(chances);
    }

    private boolean tryGenerateSchematic() {
        var manager = LoParkour.getSchematicManager();
        if (manager == null) return false;

        var all = manager.getAllSchematics();
        if (all.isEmpty()) return false;

        double diff = g.profile.get("schematicDifficulty").asDouble();
        var candidates = all.values().stream()
                .filter(s -> s.getMetadata() != null && Math.abs(s.getMetadata().getDifficulty() - diff) <= 0.26)
                .toList();
        var pool = candidates.isEmpty() ? new ArrayList<>(all.values()) : candidates;
        var schematic = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        Location origin = g.getLatest().getLocation().add(g.state.heading.clone().multiply(2));
        List<Block> placed = rotatedPaste(schematic, origin);
        if (placed.isEmpty()) return false;

        g.state.schematicBlocks.addAll(placed);
        g.state.history.addAll(placed);
        g.state.waitForSchematicCompletion = true;
        g.state.deleteSchematic = false;

        new ParkourSchematicGenerateEvent(schematic, g, g.player).call();
        return true;
    }

    private @NotNull List<Block> rotatedPaste(
            dev.loki.loparkour.schematic.lpschem.LPSchematic schematic, Location location) {
        int[] rotations = {0, 90, 180, 270};
        int rotation = rotations[ThreadLocalRandom.current().nextInt(rotations.length)];
        org.bukkit.World world = location.getWorld();
        if (world == null) return List.of();

        if (rotation == 0) return schematic.paste(location, world);
        try {
            var t = new dev.loki.loparkour.schematic.lpschem.SchematicTransformer(schematic);
            return t.rotate(rotation).paste(location, world);
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().log(Level.WARNING, "Could not rotate schematic", ex);
            return schematic.paste(location, world);
        }
    }

    private double angleInY(Vector a, Vector b) {
        return Math.atan2(a.getX() * b.getZ() - a.getZ() * b.getX(), a.dot(b));
    }
}
