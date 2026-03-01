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

            File[] files = FOLDER.listFiles((dir, name) -> name.contains("parkour-") || name.contains("spawn-island"));

            if (files == null || files.length == 0) {
                download();
                init();
                return;
            }
          
            try {
                // TODO: Migrate to LoLib schematics
                // dev.efnilite.vilib.schematic.Schematics.addFromFiles(LoParkour.getPlugin(), files);
            } catch (Exception ex) {
                LoParkour.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                        "Error while trying to load schematics", ex);
            }
        });
    }

    private static void download() {
        LoParkour.log("Downloading schematics");

        List<String> schematics = new ArrayList<>();
        schematics.addAll(Arrays.asList(SPAWN_SCHEMATICS));
        schematics.addAll(Config.SCHEMATICS.getChildren("difficulty", false).stream().map("parkour-%s"::formatted).toList());
        schematics.forEach(file -> LoParkour.getPlugin().saveResource("schematics/%s".formatted(file), true));
    }
}
