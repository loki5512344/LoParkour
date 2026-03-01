package dev.loki.loparkour.schematic.lpschem;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LPSchematicBuilder {

    private final SchematicMetadata metadata;
    private final SchematicDimensions dimensions;
    private final Map<String, Integer> paletteMap;
    private final List<String> palette;
    private final int[] blocks;
    private SchematicMarkers.Vector3i start;
    private SchematicMarkers.Vector3i end;
    private final List<SchematicMarkers.Vector3i> checkpoints;

    public LPSchematicBuilder(@NotNull String name, @NotNull String author, double difficulty,
                              int width, int height, int length) {
        this.metadata = new SchematicMetadata(name, author, difficulty);
        this.dimensions = new SchematicDimensions(width, height, length);
        this.paletteMap = new HashMap<>();
        this.palette = new ArrayList<>();
        this.blocks = new int[width * height * length];
        this.checkpoints = new ArrayList<>();

        palette.add("minecraft:air");
        paletteMap.put("minecraft:air", 0);
    }

    public LPSchematicBuilder setBlock(int x, int y, int z, @NotNull BlockData blockData) {
        String blockString = blockData.getAsString();
        int paletteIndex = getOrAddToPalette(blockString);
        int index = x + (z * dimensions.width) + (y * dimensions.width * dimensions.length);
        blocks[index] = paletteIndex;
        return this;
    }

    public LPSchematicBuilder setBlock(int x, int y, int z, @NotNull Material material) {
        return setBlock(x, y, z, material.createBlockData());
    }

    public LPSchematicBuilder setStart(int x, int y, int z) {
        this.start = new SchematicMarkers.Vector3i(x, y, z);
        return this;
    }

    public LPSchematicBuilder setEnd(int x, int y, int z) {
        this.end = new SchematicMarkers.Vector3i(x, y, z);
        return this;
    }

    public LPSchematicBuilder addCheckpoint(int x, int y, int z) {
        checkpoints.add(new SchematicMarkers.Vector3i(x, y, z));
        return this;
    }

    public LPSchematicBuilder addTag(@NotNull String tag) {
        metadata.addTag(tag);
        return this;
    }

    public LPSchematicBuilder fromWorld(@NotNull World world, @NotNull Location corner1, @NotNull Location corner2) {
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());

        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        int relX = x - minX;
                        int relY = y - minY;
                        int relZ = z - minZ;
                        setBlock(relX, relY, relZ, block.getBlockData());

                        if (block.getType() == Material.LIME_WOOL) {
                            setStart(relX, relY, relZ);
                        } else if (block.getType() == Material.RED_WOOL) {
                            setEnd(relX, relY, relZ);
                        }
                    }
                }
            }
        }

        return this;
    }

    @NotNull
    public LPSchematic build() {
        if (start == null || end == null) {
            throw new IllegalStateException("Start and end markers must be set");
        }

        SchematicMarkers markers = new SchematicMarkers(start, end);
        checkpoints.forEach(markers::addCheckpoint);

        return new LPSchematic(metadata, dimensions, palette, blocks, markers);
    }

    private int getOrAddToPalette(@NotNull String blockString) {
        if (paletteMap.containsKey(blockString)) {
            return paletteMap.get(blockString);
        }

        int index = palette.size();
        palette.add(blockString);
        paletteMap.put(blockString, index);
        return index;
    }
}
