package dev.loki.loparkour.schematic.legacy.lpschem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Read-only loader for deprecated gzip+JSON {@code .lpschem} files (format v2).
 */
public final class LPSchematicLegacy {

    private static final Gson GSON = new GsonBuilder().create();
    private static final int FORMAT_VERSION = 2;

    private final SchematicMetadata metadata;
    private final SchematicDimensions dimensions;
    private final List<String> palette;
    private final int[] blocks;

    private LPSchematicLegacy(
            SchematicMetadata metadata,
            SchematicDimensions dimensions,
            List<String> palette,
            int[] blocks
    ) {
        this.metadata = metadata;
        this.dimensions = dimensions;
        this.palette = palette;
        this.blocks = blocks;
    }

    @NotNull
    public static LPSchematicLegacy load(@NotNull File file) throws IOException {
        StringBuilder json = new StringBuilder();
        try (GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        SchematicData data = GSON.fromJson(json.toString(), SchematicData.class);
        if (data == null || data.format_version != FORMAT_VERSION) {
            throw new IOException("Unsupported .lpschem format in " + file.getName());
        }

        return new LPSchematicLegacy(data.metadata, data.dimensions, data.palette, data.blocks);
    }

    @NotNull
    public SchematicMetadata getMetadata() {
        return metadata;
    }

    @NotNull
    public SchematicDimensions getDimensions() {
        return dimensions;
    }

    @NotNull
    public List<String> getPalette() {
        return palette;
    }

    @NotNull
    public int[] getBlockArray() {
        return blocks;
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
