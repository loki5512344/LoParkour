package dev.loki.loparkour.adaptive.model;

import dev.loki.loparkour.config.core.Config;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached configuration for the adaptive difficulty system.
 * Loads settings from config.yml and caches them for performance.
 */
public class AdaptiveConfig {

    private AdaptiveConfig() {
    }

    private static final ConcurrentHashMap<String, Object> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    // Configuration keys
    private static final String KEY_ENABLED = "adaptive.enabled";
    private static final String KEY_MIN_SESSIONS = "adaptive.min-sessions";
    private static final String KEY_UPDATE_INTERVAL = "adaptive.update-interval";
    private static final String KEY_RATING_WEIGHT = "adaptive.rating-weight";
    private static final String KEY_CONFIDENCE_THRESHOLD = "adaptive.confidence-threshold";
    private static final String KEY_NEAR_MISS_DISTANCE = "adaptive.near-miss-distance";

    // Default values
    private static final boolean DEFAULT_ENABLED = true;
    private static final int DEFAULT_MIN_SESSIONS = 3;
    private static final int DEFAULT_UPDATE_INTERVAL = 300; // 5 minutes in seconds
    private static final double DEFAULT_RATING_WEIGHT = 0.15;
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.3;
    private static final double DEFAULT_NEAR_MISS_DISTANCE = 0.3;

    /**
     * Initializes the configuration cache from config.yml.
     * Should be called during plugin startup.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        CACHE.put(KEY_ENABLED, Config.CONFIG.getBoolean(KEY_ENABLED, DEFAULT_ENABLED));
        CACHE.put(KEY_MIN_SESSIONS, Config.CONFIG.getInt(KEY_MIN_SESSIONS, DEFAULT_MIN_SESSIONS));
        CACHE.put(KEY_UPDATE_INTERVAL, Config.CONFIG.getInt(KEY_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL));
        CACHE.put(KEY_RATING_WEIGHT, configDouble(KEY_RATING_WEIGHT, DEFAULT_RATING_WEIGHT));
        CACHE.put(KEY_CONFIDENCE_THRESHOLD, configDouble(KEY_CONFIDENCE_THRESHOLD, DEFAULT_CONFIDENCE_THRESHOLD));
        CACHE.put(KEY_NEAR_MISS_DISTANCE, configDouble(KEY_NEAR_MISS_DISTANCE, DEFAULT_NEAR_MISS_DISTANCE));

        initialized = true;
    }

    /**
     * Reloads configuration from config.yml.
     * Call this after config reload.
     */
    public static void reload() {
        initialized = false;
        CACHE.clear();
        init();
    }

    /**
     * Checks if adaptive difficulty system is enabled.
     *
     * @return True if enabled
     */
    public static boolean isEnabled() {
        ensureInitialized();
        return (boolean) CACHE.getOrDefault(KEY_ENABLED, DEFAULT_ENABLED);
    }

    /**
     * Gets minimum number of sessions before adaptive difficulty activates.
     *
     * @return Minimum sessions count
     */
    public static int getMinSessions() {
        ensureInitialized();
        return (int) CACHE.getOrDefault(KEY_MIN_SESSIONS, DEFAULT_MIN_SESSIONS);
    }

    /**
     * Gets the interval (in seconds) between automatic stats updates.
     *
     * @return Update interval in seconds
     */
    public static int getUpdateInterval() {
        ensureInitialized();
        return (int) CACHE.getOrDefault(KEY_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
    }

    /**
     * Gets the weight for rating updates (how much new performance affects rating).
     *
     * @return Rating weight (0.0 to 1.0)
     */
    public static double getRatingWeight() {
        ensureInitialized();
        Object value = CACHE.get(KEY_RATING_WEIGHT);
        if (value instanceof Double) {
            return (double) value;
        }
        return DEFAULT_RATING_WEIGHT;
    }

    /**
     * Gets the confidence threshold for reliable ratings.
     *
     * @return Confidence threshold (0.0 to 1.0)
     */
    public static double getConfidenceThreshold() {
        ensureInitialized();
        Object value = CACHE.get(KEY_CONFIDENCE_THRESHOLD);
        if (value instanceof Double) {
            return (double) value;
        }
        return DEFAULT_CONFIDENCE_THRESHOLD;
    }

    /**
     * Gets the distance threshold for near-miss detection.
     *
     * @return Near-miss distance in blocks
     */
    public static double getNearMissDistance() {
        ensureInitialized();
        Object value = CACHE.get(KEY_NEAR_MISS_DISTANCE);
        if (value instanceof Double) {
            return (double) value;
        }
        return DEFAULT_NEAR_MISS_DISTANCE;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    private static double configDouble(@NotNull String key, double defaultValue) {
        return Config.CONFIG.isPath(key) ? Config.CONFIG.getDouble(key) : defaultValue;
    }

    /**
     * Gets a custom config value with caching.
     *
     * @param key The config key
     * @param defaultValue Default value if key not found
     * @return The config value
     */
    @NotNull
    public static <T> T get(@NotNull String key, @NotNull T defaultValue) {
        ensureInitialized();
        @SuppressWarnings("unchecked")
        T value = (T) CACHE.get(key);
        return value != null ? value : defaultValue;
    }
}
