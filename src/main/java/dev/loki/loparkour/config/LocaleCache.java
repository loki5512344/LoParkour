package dev.loki.loparkour.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * Thread-safe cache for loaded locale configurations.
 */
public class LocaleCache {

    private final Map<String, FileConfiguration> locales = new HashMap<>();

    /**
     * Replace all cached locales with a new map.
     */
    public synchronized void setLocales(@NotNull Map<String, FileConfiguration> newLocales) {
        locales.clear();
        locales.putAll(newLocales);
    }

    /**
     * Get a value from a locale configuration, with fallback.
     */
    @NotNull
    public synchronized <T> T cachedValue(@NotNull String locale,
                                          @NotNull Function<FileConfiguration, T> extractor,
                                          @NotNull T defaultValue) {
        if (locales.isEmpty()) return defaultValue;
        FileConfiguration config = locales.get(locale);
        if (config == null) return defaultValue;
        try {
            T result = extractor.apply(config);
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get a locale configuration by key.
     */
    public synchronized FileConfiguration getLocale(@NotNull String locale) {
        return locales.get(locale);
    }

    /**
     * Check if locales are loaded.
     */
    public synchronized boolean isEmpty() {
        return locales.isEmpty();
    }

    /**
     * Get the number of loaded locales.
     */
    public synchronized int getLocaleCount() {
        return locales.size();
    }

    /**
     * Get all locale keys.
     */
    @NotNull
    public synchronized Set<String> getLocaleKeys() {
        return new HashSet<>(locales.keySet());
    }
}
