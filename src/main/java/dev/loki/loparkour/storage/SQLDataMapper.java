package dev.loki.loparkour.storage;

import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps data between Java objects and SQL result sets.
 */
public class SQLDataMapper {
    
    /**
     * Map ResultSet to Score objects.
     */
    @NotNull
    public static Map<UUID, Score> mapScores(@NotNull ResultSet rs) throws SQLException {
        Map<UUID, Score> scores = new HashMap<>();
        
        while (rs.next()) {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            String name = rs.getString("name");
            int score = rs.getInt("score");
            String time = rs.getString("time");
            String difficulty = rs.getString("difficulty");
            
            Score scoreObj = new Score(name, time, difficulty, score);
            scores.put(uuid, scoreObj);
        }
        
        return scores;
    }
    
    /**
     * Set Score parameters in PreparedStatement.
     */
    public static void setScoreParameters(@NotNull PreparedStatement stmt, @NotNull Score score, @NotNull UUID playerUuid) throws SQLException {
        stmt.setString(1, playerUuid.toString());
        stmt.setString(2, score.name());
        stmt.setInt(3, score.score());
        stmt.setString(4, score.time());
        stmt.setString(5, score.difficulty());
        stmt.setLong(6, System.currentTimeMillis());
    }
    
    /**
     * Map ResultSet to ParkourPlayer data.
     */
    @Nullable
    public static PlayerData mapPlayerData(@NotNull ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");
        String locale = rs.getString("locale");
        String styleName = rs.getString("style");
        String settings = rs.getString("settings");
        long lastSeen = rs.getLong("last_seen");
        
        return new PlayerData(uuid, name, locale, styleName, settings, lastSeen);
    }
    
    /**
     * Set ParkourPlayer parameters in PreparedStatement.
     */
    public static void setPlayerParameters(@NotNull PreparedStatement stmt, @NotNull ParkourPlayer player) throws SQLException {
        stmt.setString(1, player.player.getUniqueId().toString());
        stmt.setString(2, player.player.getName());
        stmt.setString(3, player.locale);
        stmt.setString(4, player.style);
        stmt.setString(5, serializeSettings(player));
        stmt.setLong(6, System.currentTimeMillis());
    }
    
    /**
     * Apply player data to ParkourPlayer object.
     */
    public static void applyPlayerData(@NotNull ParkourPlayer player, @NotNull PlayerData data) {
        // Sanitize legacy Boolean locale values
        String locale = data.locale();
        if (locale == null || "true".equals(locale) || "false".equals(locale) || "1".equals(locale) || "0".equals(locale)) {
            locale = "en";
        }
        player.locale = locale;
        player.style = data.styleName();

        // Apply settings if available
        if (data.settings() != null && !data.settings().isEmpty()) {
            deserializeSettings(player, data.settings());
        }
    }
    
    @NotNull
    private static String serializeSettings(@NotNull ParkourPlayer player) {
        // Simplified settings serialization
        // In real implementation, would serialize player settings to JSON
        return "{}";
    }
    
    private static void deserializeSettings(@NotNull ParkourPlayer player, @NotNull String settings) {
        // Simplified settings deserialization
        // In real implementation, would deserialize JSON to player settings
    }
    
    /**
     * Player data record for mapping.
     */
    public record PlayerData(
        UUID uuid,
        String name,
        String locale,
        String styleName,
        String settings,
        long lastSeen
    ) {}
}