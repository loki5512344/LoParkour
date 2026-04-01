package dev.loki.loparkour.schematic;

import dev.loki.loparkour.LoParkour;
import dev.lolib.scheduler.Scheduler;

import java.io.File;

/**
 * Schematic folder discovery (files are loaded on demand elsewhere).
 */
public class Schematics {

    private static final File FOLDER = LoParkour.getInFolder("schematics");

    public static void init() {
        Scheduler.get(LoParkour.getPlugin()).runAsync(() -> {
            if (!FOLDER.exists()) {
                FOLDER.mkdirs();
            }

            File[] files = FOLDER.listFiles((dir, name) -> name.endsWith(".lpschem"));

            if (files == null || files.length == 0) {
                LoParkour.log("No schematics found in " + FOLDER.getAbsolutePath());
                return;
            }

            LoParkour.log("Found " + files.length + " schematic files");
        });
    }
}
