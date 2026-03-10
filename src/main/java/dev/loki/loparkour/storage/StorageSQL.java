package dev.loki.loparkour.storage;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.player.ParkourPlayer;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MySQL storage manager.
 *
 * @since 5.0.0
 */
class StorageSQL {

    private static final SQLConnectionManager connectionManager = new SQLConnectionManager();
    private static final SQLQueryExecutor queryExecutor = new SQLQueryExecutor(connectionManager);
    private static final SQLMigrationManager migrationManager = new SQLMigrationManager(queryExecutor);

    private static boolean initialized = false;
    private static final List<String> pendingTableCreations = new CopyOnWriteArrayList<>();

    public static boolean isConnected() {
        return connectionManager.isConnected();
    }

    public static void runWhenConnected(Runnable callback) {
        connectionManager.runWhenConnected(callback);
    }

    public static void init(String mode) {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS `%s`
                (
                    uuid       CHAR(36) NOT NULL PRIMARY KEY,
                    name       VARCHAR(16),
                    time       VARCHAR(16),
                    difficulty VARCHAR(3),
                    score      INT
                )
                CHARSET = utf8 ENGINE = InnoDB;
                """.formatted(getTableName(mode));

        if (!initialized) {
            initialized = true;
            pendingTableCreations.add(createTableSql);
            Bukkit.getScheduler().runTaskAsynchronously(LoParkour.getPlugin(), StorageSQL::connect);
        } else {
            pendingTableCreations.add(createTableSql);
        }
    }

    public static void close() {
        connectionManager.close();
    }

    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        String sql = "SELECT * FROM `%s`;".formatted(getTableName(mode));
        try (PreparedStatement stmt = queryExecutor.prepareStatement(sql)) {
            if (stmt == null) return new HashMap<>();
            try (ResultSet results = stmt.executeQuery()) {
                Map<UUID, Score> scores = new HashMap<>();
                while (results.next()) {
                    scores.put(UUID.fromString(results.getString("uuid")), new Score(
                            results.getString("name"),
                            results.getString("time"),
                            results.getString("difficulty"),
                            results.getInt("score")));
                }
                return scores;
            }
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while trying to read SQL data of %s - %s".formatted(mode, ex.getMessage()));
            return new HashMap<>();
        }
    }

    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        String sql = """
                INSERT INTO `%s`
                    (uuid, name, time, difficulty, score)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name       = VALUES(name),
                                        time       = VALUES(time),
                                        difficulty = VALUES(difficulty),
                                        score      = VALUES(score);
                """.formatted(getTableName(mode));
        
        new HashMap<>(scores).forEach((uuid, score) -> {
            try (PreparedStatement stmt = queryExecutor.prepareStatement(sql)) {
                if (stmt == null) return;
                stmt.setString(1, uuid.toString());
                stmt.setString(2, score.name());
                stmt.setString(3, score.time());
                stmt.setString(4, score.difficulty());
                stmt.setInt(5, score.score());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                LoParkour.getPlugin().getLogger().severe(
                        "Error writing score for %s in mode %s - %s".formatted(uuid, mode, ex.getMessage()));
            }
        });
    }

    private static String getTableName(String mode) {
        return "%sleaderboard-%s".formatted(Option.SQL_PREFIX, mode);
    }

    public static void readPlayer(@NotNull ParkourPlayer player) {
        String sql = "SELECT * FROM `%soptions` WHERE uuid = '%s';".formatted(Option.SQL_PREFIX, player.getUUID());
        try (PreparedStatement stmt = queryExecutor.prepareStatement(sql)) {
            if (stmt == null) {
                player.setSettings(new HashMap<>());
                return;
            }
            try (ResultSet results = stmt.executeQuery()) {
                if (!results.next()) {
                    player.setSettings(new HashMap<>());
                    return;
                }

                Map<String, Object> settings = new HashMap<>();
                for (String key : ParkourPlayer.PLAYER_COLUMNS.keySet()) {
                    try {
                        settings.put(key, results.getObject(key));
                    } catch (SQLException ex) {
                        LoParkour.getPlugin().getLogger().severe(
                                "Error reading SQL data of %s, key=%s - %s".formatted(player.getName(), key, ex.getMessage()));
                    }
                }
                player.setSettings(settings);
            }
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error reading SQL data of %s - %s".formatted(player.getName(), ex.getMessage()));
        }
    }

    public static void writePlayer(@NotNull ParkourPlayer player) {
        DecimalFormat df = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        String schematicDifficulty = df.format(player.schematicDifficulty);

        String sql = """
            INSERT INTO `%soptions`
            (uuid, style, blockLead, useParticles, useSpecial, showFallMsg, showScoreboard,
             selectedTime, collectedRewards, locale, schematicDifficulty, sound)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE style               = VALUES(style),
                                    blockLead           = VALUES(blockLead),
                                    useParticles        = VALUES(useParticles),
                                    useSpecial          = VALUES(useSpecial),
                                    showFallMsg         = VALUES(showFallMsg),
                                    showScoreboard      = VALUES(showScoreboard),
                                    selectedTime        = VALUES(selectedTime),
                                    collectedRewards    = VALUES(collectedRewards),
                                    locale              = VALUES(locale),
                                    schematicDifficulty = VALUES(schematicDifficulty),
                                    sound               = VALUES(sound);
                """.formatted(Option.SQL_PREFIX);
        
        try (PreparedStatement stmt = queryExecutor.prepareStatement(sql)) {
            if (stmt == null) return;
            stmt.setString(1, player.getUUID().toString());
            stmt.setString(2, player.style);
            stmt.setInt(3, player.blockLead);
            stmt.setBoolean(4, player.particles);
            stmt.setBoolean(5, player.useSpecialBlocks);
            stmt.setBoolean(6, player.showFallMessage);
            stmt.setBoolean(7, player.showScoreboard);
            stmt.setInt(8, player.selectedTime);
            stmt.setString(9, String.join(",", player.collectedRewards));
            stmt.setString(10, player.locale);
            stmt.setString(11, schematicDifficulty);
            stmt.setBoolean(12, player.sound);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error writing player data for %s - %s".formatted(player.getName(), ex.getMessage()));
        }
    }

    private static void connect() {
        connectionManager.connect();
        migrationManager.initializeDatabase();

        for (String sql : pendingTableCreations) {
            queryExecutor.executeUpdate(sql);
        }
        pendingTableCreations.clear();
    }
}
