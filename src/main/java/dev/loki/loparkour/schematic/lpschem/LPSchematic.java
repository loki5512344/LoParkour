package dev.loki.loparkour.schematic.lpschem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LPSchematic {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FORMAT_VERSION = 2;

    private final SchematicMetadata metadata;
    private final SchematicDimensions dimensions;
    private final List<String> palette;
    private final int[] blocks;
    private final SchematicMarkers markers;
    private SchematicVisuals visuals;
    private SchematicLogic logic;

    public LPSchematic(@NotNull SchematicMetadata metadata,
                       @NotNull SchematicDimensions dimensions,
                       @NotNull List<String> palette,
                       @NotNull int[] blocks,
                       @NotNull SchematicMarkers markers) {
        this.metadata = metadata;
        this.dimensions = dimensions;
        this.palette = palette;
        this.blocks = blocks;
        this.markers = markers;
    }

    public void save(@NotNull File file) throws IOException {
        SchematicData data = new SchematicData();
        data.format_version = FORMAT_VERSION;
        data.metadata = metadata;
        data.dimensions = dimensions;
        data.palette = palette;
        data.blocks = blocks;
        data.markers = markers;
        data.visuals = visuals;
        data.logic = logic;

        String json = GSON.toJson(data);

        try (GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(file))) {
            gzip.write(json.getBytes("UTF-8"));
        }
    }

    @NotNull
    public static LPSchematic load(@NotNull File file) throws IOException {
        StringBuilder json = new StringBuilder();

        try (GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        SchematicData data = GSON.fromJson(json.toString(), SchematicData.class);

        if (data.format_version != FORMAT_VERSION) {
            throw new IOException("Unsupported format version: " + data.format_version);
        }

        LPSchematic schematic = new LPSchematic(
            data.metadata,
            data.dimensions,
            data.palette,
            data.blocks,
            data.markers
        );

        schematic.visuals = data.visuals;
        schematic.logic = data.logic;

        return schematic;
    }

    @NotNull
    public List<Block> paste(@NotNull Location origin, @NotNull World world) {
        List<Block> placedBlocks = new ArrayList<>();

        int width = dimensions.width;
        int height = dimensions.height;
        int length = dimensions.length;

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = x + (z * width) + (y * width * length);
                    int paletteIndex = blocks[index];

                    if (paletteIndex == 0) {
                        continue;
                    }

                    String blockDataString = palette.get(paletteIndex);
                    BlockData blockData = parseBlockData(blockDataString);

                    if (blockData == null) {
                        continue;
                    }

                    Block block = world.getBlockAt(
                        origin.getBlockX() + x,
                        origin.getBlockY() + y,
                        origin.getBlockZ() + z
                    );

                    block.setBlockData(blockData);
                    placedBlocks.add(block);
                }
            }
        }

        return placedBlocks;
    }

    private BlockData parseBlockData(@NotNull String blockDataString) {
        try {
            return org.bukkit.Bukkit.createBlockData(blockDataString);
        } catch (IllegalArgumentException e) {
            return Material.MAGENTA_GLAZED_TERRACOTTA.createBlockData();
        }
    }

    public SchematicMetadata getMetadata() {
        return metadata;
    }

    public SchematicDimensions getDimensions() {
        return dimensions;
    }

    public SchematicMarkers getMarkers() {
        return markers;
    }

    public SchematicVisuals getVisuals() {
        return visuals;
    }

    public void setVisuals(SchematicVisuals visuals) {
        this.visuals = visuals;
    }

    public SchematicLogic getLogic() {
        return logic;
    }

    public void setLogic(SchematicLogic logic) {
        this.logic = logic;
    }

    private static class SchematicData {
        int format_version;
        SchematicMetadata metadata;
        SchematicDimensions dimensions;
        List<String> palette;
        int[] blocks;
        SchematicMarkers markers;
        SchematicVisuals visuals;
        SchematicLogic logic;
    }
}
