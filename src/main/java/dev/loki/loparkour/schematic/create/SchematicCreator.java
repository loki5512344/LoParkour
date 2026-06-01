package dev.loki.loparkour.schematic.create;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.schematic.config.SchematicDifficultyConfig;
import dev.loki.loparkour.schematic.schem.SchematicClipboardBuilder;
import dev.loki.loparkour.schematic.schem.SchemWriter;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Saves a block selection as {@code .schem} and registers difficulty in YAML.
 */
public final class SchematicCreator {

    private SchematicCreator() {
    }

    public record CreateResult(@NotNull String stem, double difficulty, @NotNull File file) {
    }

    @NotNull
    public static CreateResult create(
            @NotNull Location pos1,
            @NotNull Location pos2,
            @Nullable String optionalStem,
            double difficulty
    ) throws IOException {
        File folder = LoParkour.getInFolder("schematics");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String stem = resolveStem(optionalStem, pos1, pos2);
        File outFile = new File(folder, stem + ".schem");
        if (outFile.exists()) {
            throw new IOException("Schematic already exists: " + outFile.getName());
        }

        Clipboard clipboard = SchematicClipboardBuilder.fromSelection(pos1, pos2);
        SchemWriter.save(clipboard, outFile);
        SchematicDifficultyConfig.setDifficulty(stem, difficulty);

        return new CreateResult(stem, difficulty, outFile);
    }

    @NotNull
    private static String resolveStem(@Nullable String optionalStem, @NotNull Location pos1, @NotNull Location pos2) {
        if (optionalStem != null && !optionalStem.isBlank()) {
            String cleaned = optionalStem.toLowerCase(Locale.ROOT);
            if (!cleaned.matches("^[a-z0-9_-]+$")) {
                throw new IllegalArgumentException("Invalid schematic name");
            }
            return cleaned.startsWith("parkour-") ? cleaned : "parkour-" + cleaned;
        }
        return "parkour-" + SchematicIdGenerator.fromSelection(pos1, pos2);
    }

    public static double parseDifficulty(@NotNull String raw) {
        double value = Double.parseDouble(raw.replace(',', '.'));
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("Difficulty must be between 0.0 and 1.0");
        }
        return value;
    }
}
