package dev.loki.loparkour.schematic.convert;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.schematic.config.SchematicDifficultyConfig;
import dev.loki.loparkour.schematic.legacy.lpschem.LPSchematicLegacy;
import dev.loki.loparkour.schematic.legacy.lpschem.SchematicMetadata;
import dev.loki.loparkour.schematic.schem.SchematicClipboardBuilder;
import dev.loki.loparkour.schematic.schem.SchemWriter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts legacy {@code .lpschem} files to {@code .schem} in the schematics folder.
 */
public final class LpschemConverter {

    private LpschemConverter() {
    }

    public record ConvertResult(int converted, int failed, @NotNull List<String> messages) {
    }

    @NotNull
    public static ConvertResult convertAll() {
        List<File> sources = new ArrayList<>();
        collect(LoParkour.getInFolder("schematics"), sources);
        collect(LoParkour.getInFolder("schematics-new"), sources);

        File outDir = LoParkour.getInFolder("schematics");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        int ok = 0;
        int failed = 0;
        List<String> messages = new ArrayList<>();

        for (File file : sources) {
            try {
                String msg = convertFile(file, outDir);
                messages.add(msg);
                ok++;
            } catch (IOException e) {
                failed++;
                messages.add(file.getName() + ": " + e.getMessage());
                LoParkour.getPlugin().getLogger().warning("lpschem convert failed: " + file.getName() + " — " + e.getMessage());
            }
        }

        if (sources.isEmpty()) {
            messages.add("No .lpschem files found in schematics/ or schematics-new/");
        }

        return new ConvertResult(ok, failed, messages);
    }

    @NotNull
    private static String convertFile(@NotNull File lpschemFile, @NotNull File outDir) throws IOException {
        LPSchematicLegacy legacy = LPSchematicLegacy.load(lpschemFile);
        SchematicMetadata meta = legacy.getMetadata();

        String stem = resolveStem(meta, lpschemFile);
        File outFile = new File(outDir, stem + ".schem");
        double difficulty = meta.getDifficulty() > 0 ? meta.getDifficulty() : 0.5;

        boolean wroteFile = false;
        if (!outFile.exists()) {
            Clipboard clipboard = SchematicClipboardBuilder.fromLegacy(legacy);
            SchemWriter.save(clipboard, outFile);
            wroteFile = true;
        }

        SchematicDifficultyConfig.setDifficulty(stem, difficulty);

        return wroteFile
                ? stem + ".schem (" + difficulty + ")"
                : stem + " (yaml updated, .schem exists)";
    }

    @NotNull
    private static String resolveStem(@NotNull SchematicMetadata meta, @NotNull File file) {
        String name = meta.getName();
        if (name != null && !name.isBlank() && name.matches("^[a-zA-Z0-9_-]+$")) {
            return name.toLowerCase(Locale.ROOT).startsWith("parkour-")
                    ? name.toLowerCase(Locale.ROOT)
                    : "parkour-" + name.toLowerCase(Locale.ROOT);
        }
        String base = file.getName();
        if (base.toLowerCase(Locale.ROOT).endsWith(".lpschem")) {
            base = base.substring(0, base.length() - 7);
        }
        if (!base.toLowerCase(Locale.ROOT).startsWith("parkour-")) {
            base = "parkour-" + base;
        }
        return base.toLowerCase(Locale.ROOT);
    }

    private static void collect(@NotNull File dir, @NotNull List<File> out) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".lpschem"));
        if (files != null) {
            for (File f : files) {
                out.add(f);
            }
        }
    }
}
