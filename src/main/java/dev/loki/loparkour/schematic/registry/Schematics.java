package dev.loki.loparkour.schematic.registry;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.schematic.core.SchematicManager;
import dev.lolib.scheduler.Scheduler;

import java.io.File;

/**
 * Ensures the schematics folder exists; structures are loaded by {@link SchematicManager}.
 */
public class Schematics {

    private Schematics() {
    }

    private static final File FOLDER = LoParkour.getInFolder("schematics");

    public static void init() {
        Scheduler.get(LoParkour.getPlugin()).runAsync(() -> {
            if (!FOLDER.exists()) {
                FOLDER.mkdirs();
            }

            File[] files = FOLDER.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".nbt") || lower.endsWith(".schem") || lower.endsWith(".schematic");
            });

            if (files == null || files.length == 0) {
                LoParkour.log("No .nbt / .schem files in " + FOLDER.getAbsolutePath());
                return;
            }

            LoParkour.log("Found " + files.length + " schematic file(s) in schematics/");
        });
    }
}
