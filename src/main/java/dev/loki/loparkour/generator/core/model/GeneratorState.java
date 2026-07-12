package dev.loki.loparkour.generator.core.model;

import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds all state data for parkour generation.
 * No logic - just data and getters/setters.
 */
public class GeneratorState {

    // Score state
    public int score = 0;
    public int totalScore = 0;
    public int schematicCooldown;
    public boolean stopped = false;

    // Location state
    public Location blockSpawn;
    public Location playerSpawn;
    public Location lastStandingPlayerLocation;
    public Vector heading;
    public Location[] zone; // [min, max] bounding box for generation area

    // Time state
    public Instant start;

    // Block history — all access is main-thread-only (Bukkit scheduler runTimer), so no sync wrapper needed.
    // Iteration is safe without external synchronization as all mutations happen on the same thread.
    public final List<Block> history = new ArrayList<>();
    public int lastPositionIndexPlayer = -1;

    // Schematic state
    public boolean deleteSchematic = false;
    public boolean waitForSchematicCompletion = false;
    public List<Block> schematicBlocks = new ArrayList<>();

    // Chance maps
    public final Map<Integer, Double> distanceChances = new HashMap<>();
    public final Map<Integer, Double> heightChances = new HashMap<>();
    public final Map<BlockData, Double> specialChances = new HashMap<>();
    public final Map<ParkourGenerator.BlockGenerationType, Double> defaultChances = new HashMap<>();

    public GeneratorState() {}

    public void resetScore() {
        score = 0;
        start = null;
    }

    public void clearHistory() {
        history.clear();
        lastPositionIndexPlayer = 0;
    }

    public void resetSchematicState() {
        waitForSchematicCompletion = false;
        deleteSchematic = true;
        schematicBlocks.clear();
    }
}
