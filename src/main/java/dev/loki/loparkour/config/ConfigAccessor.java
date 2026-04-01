package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides safe access to configuration values with validation.
 */
public class ConfigAccessor {
    
    private final FileConfiguration config;
    private final String fileName;
    private final Set<String> warnedMissingPaths = ConcurrentHashMap.newKeySet();
    
    public ConfigAccessor(@NotNull FileConfiguration config, @NotNull String fileName) {
        this.config = config;
        this.fileName = fileName;
    }
    
    /**
     * Check if path exists in configuration.
     */
    public boolean isPath(@NotNull String path) {
        return config.isSet(path);
    }
    
    /**
     * Get raw object value at path.
     */
    public Object get(@NotNull String path) {
        warnIfMissing(path);
        return config.get(path);
    }
    
    /**
     * Get boolean value at path.
     */
    public boolean getBoolean(@NotNull String path) {
        warnIfMissing(path);
        return config.getBoolean(path, false);
    }

    /**
     * Boolean at path, or default when the path is absent (no missing-path warning).
     */
    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        if (!isPath(path)) {
            return defaultValue;
        }
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * Get integer value at path.
     */
    public int getInt(@NotNull String path) {
        warnIfMissing(path);
        return config.getInt(path, 0);
    }
    
    /**
     * Get integer value at path with default.
     */
    public int getInt(@NotNull String path, int defaultValue) {
        if (!isPath(path)) {
            return defaultValue;
        }
        return config.getInt(path, defaultValue);
    }
    
    /**
     * Get double value at path.
     */
    public double getDouble(@NotNull String path) {
        warnIfMissing(path);
        return config.getDouble(path, 0.0);
    }
    
    /**
     * Get string value at path.
     */
    @NotNull
    public String getString(@NotNull String path) {
        warnIfMissing(path);
        return config.getString(path, "");
    }
    
    /**
     * Get string list at path.
     */
    @NotNull
    public List<String> getStringList(@NotNull String path) {
        warnIfMissing(path);
        return config.getStringList(path);
    }
    
    /**
     * Get integer list at path.
     */
    @NotNull
    public List<Integer> getIntList(@NotNull String path) {
        warnIfMissing(path);
        return config.getIntegerList(path);
    }
    
    /**
     * Get child keys from configuration section.
     */
    @NotNull
    public List<String> getChildren(@NotNull String path, boolean... deep) {
        if (!isPath(path)) {
            warnIfMissing(path);
            return new ArrayList<>();
        }
        
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return new ArrayList<>();
        }
        
        boolean isDeep = deep.length > 0 && deep[0];
        return new ArrayList<>(section.getKeys(isDeep));
    }
    
    private void warnIfMissing(@NotNull String path) {
        if (isPath(path)) {
            return;
        }
        if (warnedMissingPaths.add(path)) {
            LoParkour.getPlugin().getLogger().warning(
                    "Missing config path '" + path + "' in " + fileName + ". Using a safe default value.");
        }
    }
}