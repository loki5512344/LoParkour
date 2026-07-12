package dev.loki.loparkour.storage.disk;

import com.google.gson.annotations.Expose;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.leaderboard.model.Score;
import dev.loki.loparkour.player.core.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Local disk (json) storage manager.
 *
 * @since 5.0.0
 */
public class StorageDisk {

    private StorageDisk() {
    }

    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        File file = getLeaderboardFile(mode);

        if (!file.exists()) {
            return new HashMap<>();
        }

        try (FileReader reader = new FileReader(file)) {
            LeaderboardContainer read = LoParkour.getGson().fromJson(reader, LeaderboardContainer.class);

            if (read == null) {
                return new HashMap<>();
            }

            Map<UUID, String> serialized = new LinkedHashMap<>(read.serialized);
            Map<UUID, Score> scores = new HashMap<>();

            serialized.forEach((uuid, score) -> scores.put(uuid, Score.fromString(score)));

            return scores;
        } catch (IOException ex) {
                LoParkour.getPlugin().getLogger().severe(
                        "Error while trying to read leaderboard file %s".formatted(mode) + " - " + ex.getMessage());
            return new HashMap<>();
        }
    }

    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        LeaderboardContainer container = new LeaderboardContainer();
        scores.forEach((uuid, score) -> container.serialized.put(uuid, score.toString()));

        File file = getLeaderboardFile(mode);
        createFile(file);

        try (FileWriter writer = new FileWriter(file)) {
            LoParkour.getGson().toJson(container, writer);
            writer.flush();
        } catch (IOException ex) {
            LoParkour.getPlugin().getLogger().severe("Error while trying to write to leaderboard file %s".formatted(mode) + " - " + ex.getMessage());
        }
    }

    private static File getLeaderboardFile(String mode) {
        return LoParkour.getInFolder("leaderboards/%s.json".formatted(mode.toLowerCase()));
    }

    public static class LeaderboardContainer {
        @Expose
        public final Map<UUID, String> serialized = new LinkedHashMap<>();
    }

    public static void readPlayer(@NotNull ParkourPlayer player) {
        if (!getPlayerFile(player).exists()) {
            player.setSettings(new HashMap<>());
            return;
        }

        try (FileReader reader = new FileReader(getPlayerFile(player))) {
            ParkourPlayer from = LoParkour.getGson().fromJson(reader, ParkourPlayer.class);

            if (from == null) {
                LoParkour.getPlugin().getLogger().warning(
                        "Player data file for %s is empty or corrupted, using defaults".formatted(player.getName()));
                player.setSettings(new HashMap<>());
                return;
            }

            Map<String, Object> settings = new HashMap<>();

            settings.put("style", from.style);
            settings.put("blockLead", from.blockLead);
            settings.put("useParticles", from.particles);
            settings.put("useSpecial", from.useSpecialBlocks);
            settings.put("showFallMsg", from.showFallMessage);
            settings.put("showScoreboard", from.showScoreboard);
            settings.put("selectedTime", from.selectedTime);
            settings.put("collectedRewards", from.collectedRewards);

            // Sanitize legacy Boolean locale values
            String locale = from.locale;
            if (locale == null || "true".equals(locale) || "false".equals(locale) || "1".equals(locale) || "0".equals(locale)) {
                locale = "en";
            }
            settings.put("locale", locale);
            settings.put("schematicDifficulty", from.schematicDifficulty);
            settings.put("sound", from.sound);

            player.setSettings(settings);
        } catch (IOException ex) {
                LoParkour.getPlugin().getLogger().severe(
                        "Error while trying to read disk data of %s".formatted(player.getName()) + " - " + ex.getMessage());
        }
    }

    public static void writePlayer(@NotNull ParkourPlayer player) {
        File file = getPlayerFile(player);

        createFile(file);

        try (FileWriter writer = new FileWriter(file)) {
            LoParkour.getGson().toJson(player, writer);
            writer.flush();
        } catch (IOException ex) {
                LoParkour.getPlugin().getLogger().severe(
                        "Error while trying to write disk data of %s to file %s".formatted(player.getName(), file)
                                + " - " + ex.getMessage());
        }
    }

    private static void createFile(File file) {
        if (file.exists()) {
            return;
        }

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException ex) {
            LoParkour.getPlugin().getLogger().severe("Error while trying to create file %s".formatted(file) + " - " + ex.getMessage());
        }
    }

    private static File getPlayerFile(ParkourPlayer player) {
        return LoParkour.getInFolder("players/%s.json".formatted(player.getUUID()));
    }
}
