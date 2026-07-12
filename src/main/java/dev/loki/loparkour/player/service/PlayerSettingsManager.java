package dev.loki.loparkour.player.service;
import dev.loki.loparkour.player.core.ParkourPlayer;

import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.menu.core.ParkourOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Manages player settings and their mapping to database columns.
 *
 * @since 5.0.0
 */
public final class PlayerSettingsManager {

    private PlayerSettingsManager() {
    }

    private static final Map<String, OptionContainer> COLUMN_MAPPINGS = new HashMap<>();

    static {
        initializeColumnMappings();
    }

    private static void initializeColumnMappings() {
        COLUMN_MAPPINGS.put("uuid", new OptionContainer(null, null));
        COLUMN_MAPPINGS.put("style", new OptionContainer(ParkourOption.STYLES, (p, v) -> p.style = v));
        COLUMN_MAPPINGS.put("blockLead", new OptionContainer(ParkourOption.LEADS, (p, v) ->
                p.blockLead = parseIntSafe(v, defaultBlockLead())));
        COLUMN_MAPPINGS.put("useParticles", new OptionContainer(ParkourOption.PARTICLES, (p, v) -> p.particles = parseBoolean(v)));
        COLUMN_MAPPINGS.put("useSpecial", new OptionContainer(ParkourOption.SPECIAL_BLOCKS, (p, v) -> p.useSpecialBlocks = parseBoolean(v)));
        COLUMN_MAPPINGS.put("showFallMsg", new OptionContainer(ParkourOption.FALL_MESSAGE, (p, v) -> p.showFallMessage = parseBoolean(v)));
        COLUMN_MAPPINGS.put("showScoreboard", new OptionContainer(ParkourOption.SCOREBOARD, (p, v) -> p.showScoreboard = parseBoolean(v)));
        COLUMN_MAPPINGS.put("selectedTime", new OptionContainer(ParkourOption.TIME, (p, v) ->
                p.selectedTime = parseIntSafe(v, defaultSelectedTime())));
        COLUMN_MAPPINGS.put("collectedRewards", new OptionContainer(null, PlayerSettingsManager::applyCollectedRewards));
        COLUMN_MAPPINGS.put("locale", new OptionContainer(ParkourOption.LANG, (p, v) -> {
            // Sanitize legacy Boolean values from old config parsing
            String sanitized = v;
            if ("true".equals(v) || "false".equals(v)) {
                sanitized = Option.OPTIONS_DEFAULTS.getOrDefault(ParkourOption.LANG, "en");
            }
            p.locale = sanitized;
        }));
        COLUMN_MAPPINGS.put("schematicDifficulty", new OptionContainer(ParkourOption.SCHEMATICS, (p, v) ->
                p.schematicDifficulty = parseDoubleSafe(v, defaultSchematicDifficulty())));
        COLUMN_MAPPINGS.put("sound", new OptionContainer(ParkourOption.SOUND, (p, v) -> p.sound = parseBoolean(v)));
    }

    public static Map<String, OptionContainer> getColumnMappings() {
        return COLUMN_MAPPINGS;
    }

    public static void applySettings(ParkourPlayer player, Map<String, Object> settings) {
        for (String key : COLUMN_MAPPINGS.keySet()) {
            Object value = settings.get(key);
            OptionContainer container = COLUMN_MAPPINGS.get(key);

            if (container.consumer == null) {
                continue;
            }

            if (value == null || !Option.OPTIONS_ENABLED.getOrDefault(container.option, true)) {
                String defaultValue = Option.OPTIONS_DEFAULTS.getOrDefault(container.option, "");
                container.consumer.accept(player, defaultValue);
                continue;
            }

            container.consumer.accept(player, String.valueOf(value));
        }
    }

    private static boolean parseBoolean(String string) {
        return string == null || "1".equals(string) || "true".equalsIgnoreCase(string);
    }

    private static int defaultBlockLead() {
        return parseIntSafe(Option.OPTIONS_DEFAULTS.getOrDefault(ParkourOption.LEADS, "1"), 1);
    }

    private static int defaultSelectedTime() {
        return parseIntSafe(Option.OPTIONS_DEFAULTS.getOrDefault(ParkourOption.TIME, "6000"), 6000);
    }

    private static double defaultSchematicDifficulty() {
        return parseDoubleSafe(Option.OPTIONS_DEFAULTS.getOrDefault(ParkourOption.SCHEMATICS, "0.6"), 0.6);
    }

    private static int parseIntSafe(String v, int fallback) {
        if (v == null) {
            return fallback;
        }
        String s = v.trim();
        if (s.isEmpty() || "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
            return fallback;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDoubleSafe(String v, double fallback) {
        if (v == null) {
            return fallback;
        }
        String s = v.trim();
        if (s.isEmpty() || "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
            return fallback;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void applyCollectedRewards(ParkourPlayer player, String value) {
        player.collectedRewards = new ArrayList<>();

        if (!value.isEmpty()) {
            player.collectedRewards.addAll(Arrays.stream(value.replaceAll("[ \\[\\]]", "").split(","))
                    .distinct()
                    .toList());
        }
    }

    public record OptionContainer(ParkourOption option, BiConsumer<ParkourPlayer, String> consumer) {
    }
}
