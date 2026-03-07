package dev.loki.loparkour.player;

import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.menu.ParkourOption;

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
public class PlayerSettingsManager {

    private static final Map<String, OptionContainer> COLUMN_MAPPINGS = new HashMap<>();

    static {
        initializeColumnMappings();
    }

    private static void initializeColumnMappings() {
        COLUMN_MAPPINGS.put("uuid", new OptionContainer(null, null));
        COLUMN_MAPPINGS.put("style", new OptionContainer(ParkourOption.STYLES, (p, v) -> p.style = v));
        COLUMN_MAPPINGS.put("blockLead", new OptionContainer(ParkourOption.LEADS, (p, v) -> p.blockLead = Integer.parseInt(v)));
        COLUMN_MAPPINGS.put("useParticles", new OptionContainer(ParkourOption.PARTICLES, (p, v) -> p.particles = parseBoolean(v)));
        COLUMN_MAPPINGS.put("useSpecial", new OptionContainer(ParkourOption.SPECIAL_BLOCKS, (p, v) -> p.useSpecialBlocks = parseBoolean(v)));
        COLUMN_MAPPINGS.put("showFallMsg", new OptionContainer(ParkourOption.FALL_MESSAGE, (p, v) -> p.showFallMessage = parseBoolean(v)));
        COLUMN_MAPPINGS.put("showScoreboard", new OptionContainer(ParkourOption.SCOREBOARD, (p, v) -> p.showScoreboard = parseBoolean(v)));
        COLUMN_MAPPINGS.put("selectedTime", new OptionContainer(ParkourOption.TIME, (p, v) -> p.selectedTime = Integer.parseInt(v)));
        COLUMN_MAPPINGS.put("collectedRewards", new OptionContainer(null, PlayerSettingsManager::applyCollectedRewards));
        COLUMN_MAPPINGS.put("locale", new OptionContainer(ParkourOption.LANG, (p, v) -> {
            p._locale = v;
            p.locale = v;
        }));
        COLUMN_MAPPINGS.put("schematicDifficulty", new OptionContainer(ParkourOption.SCHEMATICS, (p, v) -> p.schematicDifficulty = Double.parseDouble(v)));
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
        return string == null || string.equals("1") || string.equals("true");
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
