package dev.loki.loparkour.storage;

import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Refactored StorageSQL using composition pattern.
 * Coordinates connection management, query building, and data mapping.
 */
public class StorageSQL {
    
    private static final Logger logger = Logger.getLogger(StorageSQL.class.getName());
    private static SQLConnectionManager connectionManager;
    
    /**
     * Check if database is connected.
     */
    public static boolean isConnected() {
        return connectionManager != null && connectionManager.isConnected();
    }
    
    /**
     * Run callback when database is connected.
     */
    public static void runWhenConnected(@NotNull Runnable callback) {
        if (isConnected()) {
            callback.run();
        } else {
            logger.warning("Database not connected, skipping operation");
        }
    }
    
    /**
     * Close database connection.
     */
    public static void close() {
        if (connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
    }
    
    /**
     * Initialize database connection and tables.
     */
    public static void init(@NotNull String mode) {
        try {
            connectionManager = new SQLConnectionManager();
            connectionManager.connect();
            
            createTables(mode);
            
            logger.info("Database initialized successfully for mode: " + mode);
        } catch (SQLException e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
        }
    }
    
    /**
     * Read scores for specific mode.
     */
    @NotNull
    public static Map<UUID, Score> readScores(@NotNull String mode) {
        if (!isConnected()) {
            return Map.of();
        }
        
        String tableName = SQLQueryBuilder.getTableName(mode);
        String query = SQLQueryBuilder.selectScores(tableName);
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            return SQLDataMapper.mapScores(rs);
            
        } catch (SQLException e) {
            logger.severe("Failed to read scores for mode " + mode + ": " + e.getMessage());
            return Map.of();
        }
    }
    
    /**
     * Write scores for specific mode.
     */
    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        if (!isConnected() || scores.isEmpty()) {
            return;
        }
        
        String tableName = SQLQueryBuilder.getTableName(mode);
        String query = SQLQueryBuilder.upsertScore(tableName);
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            for (Map.Entry<UUID, Score> entry : scores.entrySet()) {
                SQLDataMapper.setScoreParameters(stmt, entry.getValue(), entry.getKey());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            logger.info("Wrote " + scores.size() + " scores for mode: " + mode);
            
        } catch (SQLException e) {
            logger.severe("Failed to write scores for mode " + mode + ": " + e.getMessage());
        }
    }
    
    /**
     * Read player data from database.
     */
    public static void readPlayer(@NotNull ParkourPlayer player) {
        if (!isConnected()) {
            return;
        }
        
        String query = SQLQueryBuilder.selectPlayer();
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, player.player.getUniqueId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                SQLDataMapper.PlayerData data = SQLDataMapper.mapPlayerData(rs);
                if (data != null) {
                    SQLDataMapper.applyPlayerData(player, data);
                }
            }
            
        } catch (SQLException e) {
            logger.severe("Failed to read player data: " + e.getMessage());
        }
    }
    
    /**
     * Write player data to database.
     */
    public static void writePlayer(@NotNull ParkourPlayer player) {
        if (!isConnected()) {
            return;
        }
        
        String query = SQLQueryBuilder.upsertPlayer();
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            SQLDataMapper.setPlayerParameters(stmt, player);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.severe("Failed to write player data: " + e.getMessage());
        }
    }
    
    private static void createTables(@NotNull String mode) throws SQLException {
        String tableName = SQLQueryBuilder.getTableName(mode);
        
        try (Connection conn = connectionManager.getConnection()) {
            // Create scores table
            try (PreparedStatement stmt = conn.prepareStatement(SQLQueryBuilder.createScoresTable(tableName))) {
                stmt.executeUpdate();
            }
            
            // Create players table
            try (PreparedStatement stmt = conn.prepareStatement(SQLQueryBuilder.createPlayersTable())) {
                stmt.executeUpdate();
            }
        }
    }
}