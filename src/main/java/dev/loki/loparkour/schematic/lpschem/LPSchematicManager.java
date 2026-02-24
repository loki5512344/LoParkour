package dev.loki.loparkour.schematic.lpschem;

import dev.loki.loparkour.LoParkour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LPSchematicManager {

    private static final File SCHEMATIC_FOLDER = LoParkour.getInFolder("schematics-new");
    private final Map<String, LPSchematic> loadedSchematics = new HashMap<>();

    public LPSchematicManager() {
        if (!SCHEMATIC_FOLDER.exists()) {
            SCHEMATIC_FOLDER.mkdirs();
        }
    }

    public void loadAll() {
        File[] files = SCHEMATIC_FOLDER.listFiles((dir, name) -> name.endsWith(".lpschem"));

        if (files == null || files.length == 0) {
            LoParkour.log("No .lpschem files found");
            return;
        }

        for (File file : files) {
            try {
                LPSchematic schematic = LPSchematic.load(file);
                String name = schematic.getMetadata().getName();
                loadedSchematics.put(name, schematic);
                LoParkour.log("Loaded schematic: " + name);
            } catch (IOException e) {
                LoParkour.logging().error("Failed to load schematic: " + file.getName());
                e.printStackTrace();
            }
        }

        LoParkour.log("Loaded " + loadedSchematics.size() + " schematics");
    }

    @Nullable
    public LPSchematic getSchematic(@NotNull String name) {
        return loadedSchematics.get(name);
    }

    public void saveSchematic(@NotNull LPSchematic schematic) throws IOException {
        String fileName = schematic.getMetadata().getName().replaceAll("[^a-zA-Z0-9-_]", "_") + ".lpschem";
        File file = new File(SCHEMATIC_FOLDER, fileName);
        schematic.save(file);
        loadedSchematics.put(schematic.getMetadata().getName(), schematic);
        LoParkour.log("Saved schematic: " + schematic.getMetadata().getName());
    }

    public Map<String, LPSchematic> getAllSchematics() {
        return new HashMap<>(loadedSchematics);
    }

    public void reload() {
        loadedSchematics.clear();
        loadAll();
    }
}
