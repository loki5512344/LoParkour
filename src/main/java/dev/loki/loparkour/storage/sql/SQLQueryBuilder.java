package dev.loki.loparkour.storage.sql;

import org.jetbrains.annotations.NotNull;

/**
 * Builds SQL queries for parkour storage operations.
 */
public class SQLQueryBuilder {

    private SQLQueryBuilder() {
    }

    /**
     * Build CREATE TABLE query for scores.
     */
    @NotNull
    public static String createScoresTable(@NotNull String tableName) {
        return String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                uuid VARCHAR(36) PRIMARY KEY,
                name VARCHAR(16) NOT NULL,
                score INTEGER NOT NULL DEFAULT 0,
                time VARCHAR(32) NOT NULL DEFAULT '00:00.000',
                difficulty VARCHAR(16) NOT NULL DEFAULT '1.0',
                timestamp BIGINT NOT NULL DEFAULT 0
            )
            """, tableName);
    }
    
    /**
     * Build CREATE TABLE query for players.
     */
    @NotNull
    public static String createPlayersTable() {
        return """
            CREATE TABLE IF NOT EXISTS players (
                uuid VARCHAR(36) PRIMARY KEY,
                name VARCHAR(16) NOT NULL,
                locale VARCHAR(8) NOT NULL DEFAULT 'en',
                style VARCHAR(32) NOT NULL DEFAULT 'default',
                settings TEXT,
                last_seen BIGINT NOT NULL DEFAULT 0
            )
            """;
    }
    
    /**
     * Build SELECT query for scores.
     */
    @NotNull
    public static String selectScores(@NotNull String tableName) {
        return String.format(
            "SELECT uuid, name, score, time, difficulty, timestamp FROM %s ORDER BY score DESC",
            tableName
        );
    }
    
    /**
     * Build INSERT/UPDATE query for scores.
     */
    @NotNull
    public static String upsertScore(@NotNull String tableName) {
        return String.format("""
            INSERT INTO %s (uuid, name, score, time, difficulty, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                score = VALUES(score),
                time = VALUES(time),
                difficulty = VALUES(difficulty),
                timestamp = VALUES(timestamp)
            """, tableName);
    }
    
    /**
     * Build SELECT query for player data.
     */
    @NotNull
    public static String selectPlayer() {
        return "SELECT uuid, name, locale, style, settings, last_seen FROM players WHERE uuid = ?";
    }
    
    /**
     * Build INSERT/UPDATE query for player data.
     */
    @NotNull
    public static String upsertPlayer() {
        return """
            INSERT INTO players (uuid, name, locale, style, settings, last_seen)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                locale = VALUES(locale),
                style = VALUES(style),
                settings = VALUES(settings),
                last_seen = VALUES(last_seen)
            """;
    }
    
    /**
     * Get table name for specific mode.
     */
    @NotNull
    public static String getTableName(@NotNull String mode) {
        return "scores_" + mode.toLowerCase().replace(" ", "_");
    }
}
