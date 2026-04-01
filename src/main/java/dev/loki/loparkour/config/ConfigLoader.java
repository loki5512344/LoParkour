package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles loading, updating, and basic validation of configuration files.
 */
public class ConfigLoader {

    @NotNull
    public static FileConfiguration loadConfig(@NotNull File configFile) {
        return YamlConfiguration.loadConfiguration(configFile);
    }

    @NotNull
    public static File initializeConfigFile(@NotNull String fileName) {
        File configFile = LoParkour.getInFolder(fileName);
        if (!configFile.exists()) {
            LoParkour.getPlugin().saveResource(fileName, false);
        }
        return configFile;
    }

    public static void updateConfig(@NotNull String fileName, @NotNull File configFile, @Nullable List<String> ignoredSections) {
        try {
            var plugin = LoParkour.getPlugin();
            var resourceStream = plugin.getResource(fileName);
            if (resourceStream != null) {
                ConfigUpdater.update(configFile, resourceStream, "1.0", ignoredSections, plugin.getLogger());
            }
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "Error while trying to update config file: " + fileName, ex);
        }
    }

    public static void reloadAllConfigs(boolean initialLoad) {
        for (Config config : Config.values()) {
            if (initialLoad) {
                config.path = initializeConfigFile(config.fileName);
                updateConfig(config.fileName, config.path, config.ignoredSections);
            }
            config.fileConfiguration = loadConfig(config.path);
        }

        validateConfigs();
    }

    public static void initializeDependentSystems(boolean firstLoad) {
        dev.loki.loparkour.reward.Rewards.init();
        Locales.init();
        dev.loki.loparkour.schematic.Schematics.init();
        Option.init(firstLoad);
    }

    // ── inline validation (was ConfigValidatorRefactored) ─────────────────────

    private static void validateConfigs() {
        Logger log = LoParkour.getPlugin().getLogger();

        if (Config.CONFIG.isPath("world.max-y") && Config.CONFIG.isPath("world.min-y")) {
            int maxY = Config.CONFIG.getInt("world.max-y");
            int minY = Config.CONFIG.getInt("world.min-y");
            if (maxY <= minY) {
                log.severe("Invalid config: world.max-y (%d) must be > world.min-y (%d)".formatted(maxY, minY));
            }
        }

        if (Config.GENERATION.isPath("generation.normal.height")) {
            double sum = Config.GENERATION.getDouble("generation.normal.height.1")
                       + Config.GENERATION.getDouble("generation.normal.height.0")
                       + Config.GENERATION.getDouble("generation.normal.height.-1")
                       + Config.GENERATION.getDouble("generation.normal.height.-2");
            if (sum <= 0) {
                log.severe("Invalid config: generation height chances sum to %.1f, must be > 0".formatted(sum));
            }
        }
    }
}
