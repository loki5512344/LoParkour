package dev.loki.loparkour.schematic;

import dev.lolib.scheduler.Scheduler;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.lolib.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores schematics, so they don't have to be read every time.
 */
public class Schematics {

    private static final String[] SPAWN_SCHEMATICS = new String[]{
            "spawn-island", "spawn-island-duels"
    };
    private static final File FOLDER = LoParkour.getInFolder("schematics");

    /**
     * Reads all files.
     */
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
