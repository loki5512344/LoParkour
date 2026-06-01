package dev.loki.loparkour.adaptive.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.adaptive.model.PlayerMetrics;
import dev.loki.loparkour.adaptive.model.SkillRating;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * JSON file-based implementation of StatsRepository.
 * Stores data in playerdata/<uuid>.json files.
 * Saves only when player leaves to minimize I/O.
 */
public class FileStatsStorage implements StatsRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFolder;

    public FileStatsStorage() {
        this.dataFolder = new File(LoParkour.getPlugin().getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    @Nullable
    public SkillRating loadSkillRating(@NotNull UUID playerUuid) {
        JsonObject data = loadPlayerData(playerUuid);
        if (data == null || !data.has("skillRating")) {
            return null;
        }

        JsonObject ratingObj = data.getAsJsonObject("skillRating");
        double rating = ratingObj.get("rating").getAsDouble();
        double confidence = ratingObj.get("confidence").getAsDouble();
        int sessions = ratingObj.get("sessionsCount").getAsInt();

        return new SkillRating(playerUuid, rating, confidence, sessions);
    }

    @Override
    public void saveSkillRating(@NotNull SkillRating rating) {
        JsonObject data = loadPlayerData(rating.getPlayerUuid());
        if (data == null) {
            data = new JsonObject();
        }

        JsonObject ratingObj = new JsonObject();
        ratingObj.addProperty("rating", rating.getRating());
        ratingObj.addProperty("confidence", rating.getConfidence());
        ratingObj.addProperty("sessionsCount", rating.getSessionsCount());
        ratingObj.addProperty("lastUpdated", rating.getLastUpdated());

        data.add("skillRating", ratingObj);
        savePlayerData(rating.getPlayerUuid(), data);
    }

    @Override
    @Nullable
    public PlayerMetrics loadMetrics(@NotNull UUID playerUuid) {
        JsonObject data = loadPlayerData(playerUuid);
        if (data == null || !data.has("metrics")) {
            return null;
        }

        JsonObject metricsObj = data.getAsJsonObject("metrics");
        PlayerMetrics metrics = new PlayerMetrics(playerUuid);

        metrics.setAvgTimePerBlock(metricsObj.get("avgTimePerBlock").getAsDouble());
        metrics.setNearMissCount(metricsObj.get("nearMissCount").getAsInt());
        metrics.setTotalJumps(metricsObj.get("totalJumps").getAsInt());
        metrics.setLastUpdated(metricsObj.get("lastUpdated").getAsLong());

        JsonObject jumpStats = metricsObj.getAsJsonObject("jumpTypeStats");
        for (Map.Entry<String, com.google.gson.JsonElement> entry : jumpStats.entrySet()) {
            metrics.putJumpTypeStat(entry.getKey(), entry.getValue().getAsInt());
        }

        return metrics;
    }

    @Override
    public void saveMetrics(@NotNull PlayerMetrics metrics) {
        JsonObject data = loadPlayerData(metrics.getPlayerUuid());
        if (data == null) {
            data = new JsonObject();
        }

        JsonObject metricsObj = new JsonObject();
        metricsObj.addProperty("avgTimePerBlock", metrics.getAvgTimePerBlock());
        metricsObj.addProperty("nearMissCount", metrics.getNearMissCount());
        metricsObj.addProperty("totalJumps", metrics.getTotalJumps());
        metricsObj.addProperty("lastUpdated", metrics.getLastUpdated());

        JsonObject jumpStats = new JsonObject();
        for (Map.Entry<String, Integer> entry : metrics.getJumpTypeStats().entrySet()) {
            jumpStats.addProperty(entry.getKey(), entry.getValue());
        }
        metricsObj.add("jumpTypeStats", jumpStats);

        data.add("metrics", metricsObj);
        savePlayerData(metrics.getPlayerUuid(), data);
    }

    @Override
    public void incrementJumps(@NotNull UUID playerUuid, int amount) {
        JsonObject data = loadPlayerData(playerUuid);
        if (data == null) {
            data = new JsonObject();
        }

        int current = data.has("totalJumps") ? data.get("totalJumps").getAsInt() : 0;
        data.addProperty("totalJumps", current + amount);
        savePlayerData(playerUuid, data);
    }

    @Override
    public void incrementFalls(@NotNull UUID playerUuid, int amount) {
        JsonObject data = loadPlayerData(playerUuid);
        if (data == null) {
            data = new JsonObject();
        }

        int current = data.has("totalFalls") ? data.get("totalFalls").getAsInt() : 0;
        data.addProperty("totalFalls", current + amount);
        savePlayerData(playerUuid, data);
    }

    @Override
    public void updateLongestStreak(@NotNull UUID playerUuid, int streak) {
        JsonObject data = loadPlayerData(playerUuid);
        if (data == null) {
            data = new JsonObject();
        }

        int current = data.has("longestStreak") ? data.get("longestStreak").getAsInt() : 0;
        if (streak > current) {
            data.addProperty("longestStreak", streak);
            savePlayerData(playerUuid, data);
        }
    }

    @Override
    public void close() {
        // No resources to close for file storage
    }

    @Nullable
    private JsonObject loadPlayerData(@NotNull UUID playerUuid) {
        File file = new File(dataFolder, playerUuid.toString() + ".json");
        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            LoParkour.getPlugin().getLogger().warning(
                    "Failed to load player data for " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }

    private void savePlayerData(@NotNull UUID playerUuid, @NotNull JsonObject data) {
        File file = new File(dataFolder, playerUuid.toString() + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LoParkour.getPlugin().getLogger().warning(
                    "Failed to save player data for " + playerUuid + ": " + e.getMessage());
        }
    }
}
