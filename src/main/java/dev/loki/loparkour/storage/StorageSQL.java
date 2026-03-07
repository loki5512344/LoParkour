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
        new HashMap<>(scores).forEach((uuid, score) -> queryExecutor.executeUpdate("""
                INSERT INTO `%s`
                    (uuid, name, time, difficulty, score)
                VALUES ('%s', '%s', '%s', '%s', %d)
                ON DUPLICATE KEY UPDATE name       = '%s',
                                        time       = '%s',
                                        difficulty = '%s',
                                        score      = %d;
                """.formatted(getTableName(mode), uuid, score.name(), score.time(), score.difficulty(), score.score(),
                score.name(), score.time(), score.difficulty(), score.score())));
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

        queryExecutor.executeUpdate("""
            INSERT INTO `%soptions`
            (uuid, style, blockLead, useParticles, useSpecial, showFallMsg, showScoreboard,
             selectedTime, collectedRewards, locale, schematicDifficulty, sound)
            VALUES ('%s', '%s', %d, %b, %b, %b, %b, %d, '%s', '%s', %s, %b)
            ON DUPLICATE KEY UPDATE style               = '%s',
                                    blockLead           = %d,
                                    useParticles        = %b,
                                    useSpecial          = %b,
                                    showFallMsg         = %b,
                                    showScoreboard      = %b,
                                    selectedTime        = %d,
                                    collectedRewards    = '%s',
                                    locale              = '%s',
                                    schematicDifficulty = %s,
                                    sound               = %b;
                """.formatted(Option.SQL_PREFIX,
                player.getUUID(), player.style, player.blockLead,
                player.particles, player.useSpecialBlocks, player.showFallMessage,
                player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards),
                player.locale, schematicDifficulty, player.sound,
                player.style, player.blockLead,
                player.particles, player.useSpecialBlocks, player.showFallMessage,
                player.showScoreboard, player.selectedTime, String.join(",", player.collectedRewards),
                player.locale, schematicDifficulty, player.sound));
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
