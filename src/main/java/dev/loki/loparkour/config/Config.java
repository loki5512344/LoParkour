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

        // read config stuff
        Rewards.init();
        Locales.init();
        Schematics.init();
        Option.init(initialLoad);

        LoParkour.log("Loaded all config files");
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
     */
    public void update() {
        try {
            // TODO: ConfigUpdater.update(LoParkour.getPlugin(), fileName, path, ignoredSections);
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
