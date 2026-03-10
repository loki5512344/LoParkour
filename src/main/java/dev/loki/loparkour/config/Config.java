package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.reward.Rewards;
import dev.loki.loparkour.schematic.Schematics;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Config management class.
 */
public enum Config {

    CONFIG("config.yml", List.of("styles")),
    GENERATION("generation.yml", null),
    REWARDS("rewards-v2.yml", List.of("score-rewards", "interval-rewards", "one-time-rewards")),
    SCHEMATICS("schematics/schematics.yml", List.of("difficulty"));

    /**
     * The path to this file, incl. plugin folder.
     * Not final — resolved lazily on first reload() call after plugin instance is set.
     */
    public File path;
    /**
     * The name of this file, e.g. config.yml
     */
    public final String fileName;
    /**
     * The sections in the file that will be ignored when updating the keys.
     */
    public final List<String> ignoredSections;
    /**
     * The {@link FileConfiguration} instance associated with this config file.
     */
    public FileConfiguration fileConfiguration;

    Config(String fileName, @Nullable List<String> ignoredSections) {
        this.fileName = fileName;
        this.ignoredSections = ignoredSections;
        // path resolved lazily via getPath() — do NOT call LoParkour.getInFolder() here,
        // because enum constants are initialized before onEnable() sets the instance.
        this.path = null; // placeholder; real path resolved on first access
    }



    /**
     * Reloads all config files.
     * On first call (initialLoad=true) also resolves file paths and saves defaults.
     */
    public static void reload(boolean initialLoad) {
        for (Config config : values()) {
            if (initialLoad) {
                // Resolve path now that instance is guaranteed to be set
                config.path = LoParkour.getInFolder(config.fileName);
                if (!config.path.exists()) {
                    LoParkour.getPlugin().saveResource(config.fileName, false);
                }
                config.update();
            }
            config.load();
        }

        // Validate configuration values
        validateConfigs();

        // read config stuff
        Rewards.init();
        Locales.init();
        Schematics.init();
        Option.init(initialLoad);
    }

    /**
     * Validates critical configuration values to prevent crashes and unexpected behavior.
     */
    private static void validateConfigs() {
        var logger = LoParkour.getPlugin().getLogger();
        
        // Validate border-size
        if (CONFIG.isPath("world.border-size")) {
            int borderSize = CONFIG.getInt("world.border-size");
            if (borderSize <= 0) {
                logger.severe("Invalid config: world.border-size must be > 0, found: " + borderSize);
                logger.severe("Using default value: 1000");
            }
        }
        
        // Validate world dimensions
        if (CONFIG.isPath("world.max-y") && CONFIG.isPath("world.min-y")) {
            int maxY = CONFIG.getInt("world.max-y");
            int minY = CONFIG.getInt("world.min-y");
            if (maxY <= minY) {
                logger.severe("Invalid config: world.max-y (" + maxY + ") must be > world.min-y (" + minY + ")");
                logger.severe("Using default values: max-y=320, min-y=-64");
            }
        }
        
        // Validate section dimensions
        if (CONFIG.isPath("world.section.width") && CONFIG.isPath("world.section.length")) {
            int width = CONFIG.getInt("world.section.width");
            int length = CONFIG.getInt("world.section.length");
            if (width <= 0 || length <= 0) {
                logger.severe("Invalid config: world.section dimensions must be > 0, found: width=" + width + ", length=" + length);
                logger.severe("Using default values: width=50, length=50");
            }
        }
        
        // Validate generation chances (must sum to reasonable values)
        if (GENERATION.isPath("chances.height")) {
            double sum = GENERATION.getDouble("chances.height.1") + 
                        GENERATION.getDouble("chances.height.0") + 
                        GENERATION.getDouble("chances.height.-1") + 
                        GENERATION.getDouble("chances.height.-2");
            if (sum <= 0) {
                logger.severe("Invalid config: generation height chances sum to " + sum + ", must be > 0");
                logger.severe("Check generation.yml chances.height section");
            }
        }
        
        // Validate ghost mode settings
        if (CONFIG.isPath("ghost-mode.show-top")) {
            int showTop = CONFIG.getInt("ghost-mode.show-top");
            if (showTop < 0 || showTop > 10) {
                logger.warning("Config: ghost-mode.show-top should be between 0-10, found: " + showTop);
            }
        }
        
        // Validate cleanup settings
        if (GENERATION.isPath("advanced.cleanup-distance")) {
            int cleanupDistance = GENERATION.getInt("advanced.cleanup-distance");
            if (cleanupDistance <= 0) {
                logger.severe("Invalid config: advanced.cleanup-distance must be > 0, found: " + cleanupDistance);
                logger.severe("Using default value: 100");
            }
        }
    }

    /**
     * Loads the file from disk.
     */
    public void load() {
        if (path == null) return; // not yet initialized, skip
        this.fileConfiguration = YamlConfiguration.loadConfiguration(path);
    }

    /**
     * Updates the file so all keys are present.
     * Any key that exists in the bundled default but is missing on disk
     * will be appended to the correct section automatically.
     */
    public void update() {
        try {
            ConfigUpdater.update(LoParkour.getPlugin(), fileName, path, ignoredSections);
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "Error while trying to update config file: " + fileName, ex);
        }
    }

    /**
     * @param path The path.
     * @return True when path exists, false if not.
     */
    public boolean isPath(@NotNull String path) {
        return fileConfiguration.isSet(path);
    }

    /**
     * @param path The path.
     * @return The value at path.
     */
    public Object get(@NotNull String path) {
        check(path);

        return fileConfiguration.get(path);
    }

    /**
     * @param path The path.
     * @return The boolean value at path.
     */
    public boolean getBoolean(@NotNull String path) {
        check(path);

        return fileConfiguration.getBoolean(path);
    }

    /**
     * @param path The path.
     * @return The int value at path.
     */
    public int getInt(@NotNull String path) {
        check(path);

        return fileConfiguration.getInt(path);
    }

    /**
     * @param path The path.
     * @return The double value at path.
     */
    public double getDouble(@NotNull String path) {
        check(path);

        return fileConfiguration.getDouble(path);
    }

    /**
     * @param path The path.
     * @return The String value at path.
     */
    @NotNull
    public String getString(@NotNull String path) {
        check(path);

        return fileConfiguration.getString(path, "");
    }

    /**
     * @param path The path.
     * @return The String list value at path.
     */
    @NotNull
    public List<String> getStringList(@NotNull String path) {
        check(path);

        return fileConfiguration.getStringList(path);
    }

    /**
     * @param path The path.
     * @return The int list value at path.
     */
    @NotNull
    public List<Integer> getIntList(@NotNull String path) {
        check(path);

        return fileConfiguration.getIntegerList(path);
    }

    /**
     * @param path The path.
     * @param deep Whether search should include children of children as well.
     * @return The children nodes from path.
     */
    @NotNull
    public List<String> getChildren(@NotNull String path, boolean... deep) {
        check(path);

        ConfigurationSection section = fileConfiguration.getConfigurationSection(path);

        if (section == null) {
            return new ArrayList<>();
        }

        boolean isDeep = deep.length > 0 && deep[0];
        return new ArrayList<>(section.getKeys(isDeep));
    }

    // checks if the specified path exists to avoid developer error
    private void check(@NotNull String path) {
        if (!isPath(path)) {
            throw new NoSuchElementException("Unknown path %s in %s".formatted(path, fileName));
        }
    }
}
