package dev.loki.loparkour.schematic.config;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.schematic.core.SchematicManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Writes entries to {@code schematics/schematics.yml} and reloads {@link Config#SCHEMATICS}.
 */
public final class SchematicDifficultyConfig {

    private SchematicDifficultyConfig() {
    }

    public static void setDifficulty(@NotNull String fileStem, double difficulty) throws IOException {
        if (Config.SCHEMATICS.path == null) {
            throw new IOException("Schematics config path is not initialized");
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(Config.SCHEMATICS.path);
        String key = SchematicManager.configKey(fileStem);
        yaml.set("difficulty." + key, difficulty);
        yaml.save(Config.SCHEMATICS.path);

        Config.SCHEMATICS.fileConfiguration = YamlConfiguration.loadConfiguration(Config.SCHEMATICS.path);
        Config.SCHEMATICS.load();
    }
}
