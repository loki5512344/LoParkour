package dev.loki.loparkour.generator.profile;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.generator.core.model.Profile;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Loads default generation weights from generation.yml into a {@link Profile}.
 */
public final class GenerationProfileLoader {

    private static final Set<String> PLAYER_KEYS = Set.of(
            "schematicDifficulty", "blockLead", "particles", "sound",
            "useSpecialBlocks", "showFallMessage", "showScoreboard",
            "selectedTime", "style"
    );

    private GenerationProfileLoader() {
    }

    @NotNull
    public static Profile loadFromConfig() {
        Profile profile = new Profile();

        int minDistance = Config.GENERATION.getInt("advanced.distance.min", 1);
        int maxDistance = Config.GENERATION.getInt("advanced.distance.max", 4);
        profile.set("distance.min", String.valueOf(minDistance));
        profile.set("distance.max", String.valueOf(maxDistance));
        for (int distance = minDistance; distance <= maxDistance; distance++) {
            int chance = Config.GENERATION.getInt("advanced.distance.chances." + distance, 1);
            profile.set("distance.chances." + distance, String.valueOf(chance));
        }

        int minHeight = Config.GENERATION.getInt("advanced.height.min", -1);
        int maxHeight = Config.GENERATION.getInt("advanced.height.max", 2);
        profile.set("height.min", String.valueOf(minHeight));
        profile.set("height.max", String.valueOf(maxHeight));
        for (int height = minHeight; height <= maxHeight; height++) {
            int chance = Config.GENERATION.getInt("advanced.height.chances." + height, 1);
            profile.set("height.chances." + height, String.valueOf(chance));
        }

        profile.set("type.default", String.valueOf(Config.GENERATION.getInt("advanced.type.default", 80)));
        profile.set("type.special", String.valueOf(Config.GENERATION.getInt("advanced.type.special", 15)));
        profile.set("type.schematic", String.valueOf(Config.GENERATION.getInt("advanced.type.schematic", 5)));

        return profile;
    }

    public static void preservePlayerSettings(@NotNull Profile from, @NotNull Profile to) {
        for (String key : PLAYER_KEYS) {
            if (from.settings.containsKey(key)) {
                to.set(key, from.get(key).value());
            }
        }
    }

    /** Replaces all settings on {@code target} with a snapshot from {@code source}. */
    public static void copyInto(@NotNull Profile target, @NotNull Profile source) {
        target.settings.clear();
        for (Map.Entry<String, Profile.ProfileValue> entry : source.settings.entrySet()) {
            target.set(entry.getKey(), entry.getValue().value());
        }
    }
}
