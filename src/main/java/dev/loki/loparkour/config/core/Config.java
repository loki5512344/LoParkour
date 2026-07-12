package dev.loki.loparkour.config.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Refactored Config enum using composition pattern.
 * Delegates functionality to specialized components.
 */
public enum Config {

    CONFIG("config.yml", List.of("styles")),
    GENERATION("generation.yml", null),
    REWARDS("rewards-v2.yml", List.of("score-rewards", "interval-rewards", "one-time-rewards")),
    SCHEMATICS("schematics/schematics.yml", List.of("difficulty"));

    public File path;
    public final String fileName;
    public final List<String> ignoredSections;
    public FileConfiguration fileConfiguration;
    
    private ConfigAccessor accessor;

    Config(String fileName, @Nullable List<String> ignoredSections) {
        this.fileName = fileName;
        this.ignoredSections = ignoredSections;
        this.path = null; // Resolved lazily
    }

    /**
     * Reload all configuration files.
     */
    public static void reload(boolean initialLoad) {
        ConfigLoader.reloadAllConfigs(initialLoad);
        
        // Update accessors for all configs
        for (Config config : values()) {
            if (config.fileConfiguration != null) {
                config.accessor = new ConfigAccessor(config.fileConfiguration, config.fileName);
            }
        }

        // Now that accessors exist, initialize systems that depend on Config getters
        ConfigLoader.initializeDependentSystems(initialLoad);
    }

    /**
     * Load this configuration file from disk.
     */
    public void load() {
        if (path == null) {
            return;
        }
        
        this.fileConfiguration = ConfigLoader.loadConfig(path);
        this.accessor = new ConfigAccessor(fileConfiguration, fileName);
    }

    /**
     * Update this configuration file with new keys.
     */
    public void update() {
        if (path != null) {
            ConfigLoader.updateConfig(fileName, path, ignoredSections);
        }
    }

    // Delegate all accessor methods to ConfigAccessor
    
    public boolean isPath(@NotNull String path) {
        return accessor != null && accessor.isPath(path);
    }

    public Object get(@NotNull String path) {
        return accessor.get(path);
    }

    public boolean getBoolean(@NotNull String path) {
        return accessor.getBoolean(path);
    }

    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        return accessor != null ? accessor.getBoolean(path, defaultValue) : defaultValue;
    }

    public int getInt(@NotNull String path) {
        return accessor.getInt(path);
    }
    
    public int getInt(@NotNull String path, int defaultValue) {
        return accessor != null ? accessor.getInt(path, defaultValue) : defaultValue;
    }

    public double getDouble(@NotNull String path) {
        return accessor.getDouble(path);
    }

    @NotNull
    public String getString(@NotNull String path) {
        return accessor.getString(path);
    }

    @NotNull
    public List<String> getStringList(@NotNull String path) {
        return accessor.getStringList(path);
    }

    @NotNull
    public List<Integer> getIntList(@NotNull String path) {
        return accessor.getIntList(path);
    }

    @NotNull
    public List<String> getChildren(@NotNull String path, boolean... deep) {
        return accessor.getChildren(path, deep);
    }
}
