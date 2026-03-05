package dev.loki.loparkour.generator;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourBlockGenerateEvent;
import dev.loki.loparkour.api.event.ParkourSchematicGenerateEvent;
import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
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
        if (g.waitForSchematicCompletion) return;

        Map<ParkourGenerator.BlockGenerationType, Double> chances = new HashMap<>(g.defaultChances);
        if (g.schematicCooldown > 0
                || g.generatorOptions.contains(GeneratorOption.DISABLE_SCHEMATICS)
                || g.profile.get("schematicDifficulty").asDouble() == 0.0
                || !g.schematicBlocks.isEmpty()) {
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
        g.playerSpawn = spawn;
        g.lastStandingPlayerLocation = spawn;
        g.blockSpawn = blockSpawn;
        g.history.add(blockSpawn.getBlock());
        generate(g.profile.get("blockLead").asInt());
    }

    /** Clears schematic blocks and resets cooldown. */
    public void deleteSchematic() {
        if (!g.deleteSchematic) return;
        g.schematicBlocks.forEach(b -> b.setType(Material.AIR));
        g.schematicBlocks.clear();
        g.deleteSchematic = false;
        g.schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
    }

    // ── Block selection ────────────────────────────────────────────────────────

    protected List<Block> selectBlocks() {
        int height   = Probs.random(g.heightChances);
        int distance = Probs.random(g.distanceChances);
        return List.of(selectNext(g.getLatest(), distance, height));
    }

    protected Block selectNext(Block current, int distance, int height) {
        JumpDirector director = new JumpDirector(
            BoundingBox.of(g.zone[0], g.zone[1]), g.getLatest().getLocation().toVector());

        g.heading = director.getRecommendedHeading(g.heading);
        height    = director.getRecommendedHeight(height);

        switch (g.getLatest().getType()) {
            case SMOOTH_QUARTZ_SLAB -> height   = Math.min(height, 0);
            case GLASS_PANE         -> distance = Math.min(distance, 3);
        }
        if (height > 0) distance = Math.max(distance - height, 1);

        double sd = g.generatorOptions.contains(GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE) ? 0.5 : 1;
        int randomOffset = new JumpOffsetGenerator(height, distance).getRandomOffset(0, sd);

        Vector offset = g.heading.clone().multiply(distance + 1).setY(height);
        if (offset.getX() == 0) offset.setX(randomOffset);
        else                    offset.setZ(randomOffset);

        offset.rotateAroundY(angleInY(g.heading, Option.HEADING.getDirection()));
        return current.getLocation().add(offset).getBlock();
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
            BlockData data = (jump == ParkourGenerator.BlockGenerationType.SPECIAL
                    && !g.generatorOptions.contains(GeneratorOption.DISABLE_SPECIAL))
                    ? Probs.random(g.specialChances)
                    : selectBlockData();

            if (data instanceof Fence) block = block.getLocation().subtract(0, 1, 0).getBlock();
            block.setBlockData(data, data instanceof Fence || data instanceof GlassPane);
            placed.add(block);
        }

        new ParkourBlockGenerateEvent(placed, g, g.player).call();
        g.effects.particles(placed);
        g.effects.sound(placed);
        g.history.addAll(placed);
        g.schematicCooldown--;
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
        var schematic = pool.get(new java.util.Random().nextInt(pool.size()));

        Location origin = g.getLatest().getLocation().add(g.heading.clone().multiply(2));
        List<Block> placed = rotatedPaste(schematic, origin);
        if (placed.isEmpty()) return false;

        g.schematicBlocks.addAll(placed);
        g.history.addAll(placed);
        g.waitForSchematicCompletion = true;
        g.deleteSchematic = false;

        new ParkourSchematicGenerateEvent(schematic, g, g.player).call();
        return true;
    }

    private @NotNull List<Block> rotatedPaste(
            dev.loki.loparkour.schematic.lpschem.LPSchematic schematic, Location location) {
        int[] rotations = {0, 90, 180, 270};
        int rotation = rotations[new java.util.Random().nextInt(rotations.length)];
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
