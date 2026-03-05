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

    private static Connection connection;
    private static boolean initialized = false;
    private static volatile boolean connected = false;

    // Tables queued to create after connection is established
    private static final List<String> pendingTableCreations = new CopyOnWriteArrayList<>();

    // Callbacks to run after connection is ready (e.g. initial leaderboard reads)
    private static final List<Runnable> onConnectCallbacks = new CopyOnWriteArrayList<>();

    /**
     * Returns true if the SQL connection is ready.
     */
    public static boolean isConnected() {
        return connected;
    }

    /**
     * Runs the given callback immediately if already connected, or defers it until connect() succeeds.
     */
    public static void runWhenConnected(Runnable callback) {
        if (connected) {
            callback.run();
        } else {
            onConnectCallbacks.add(callback);
        }
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
            // Queue this table for creation after connect() finishes
            pendingTableCreations.add(createTableSql);
            // Connect async — table creation happens inside connect() after connection is ready
            Bukkit.getScheduler().runTaskAsynchronously(LoParkour.getPlugin(), StorageSQL::connect);
        } else {
            // Already connected (or connecting) — send directly
            pendingTableCreations.add(createTableSql);
        }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LoParkour.log("Closed connection to MySQL");
            }
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe("Error while trying to close connection to SQL database - " + ex.getMessage());
        }
    }

    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        String sql = "SELECT * FROM `%s`;".formatted(getTableName(mode));
        try (PreparedStatement stmt = prepareStatement(sql)) {
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
            LoParkour.getPlugin().getLogger().severe("Error while trying to read SQL data of %s - %s".formatted(mode, ex.getMessage()));
            return new HashMap<>();
        }
    }

    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        new HashMap<>(scores).forEach((uuid, score) -> sendUpdate("""
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
        try (PreparedStatement stmt = prepareStatement(sql)) {
            if (stmt == null) { player.setSettings(new HashMap<>()); return; }
            try (ResultSet results = stmt.executeQuery()) {
                if (!results.next()) { player.setSettings(new HashMap<>()); return; }

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
            LoParkour.getPlugin().getLogger().severe("Error reading SQL data of %s - %s".formatted(player.getName(), ex.getMessage()));
        }
    }

    public static void writePlayer(@NotNull ParkourPlayer player) {
        DecimalFormat df = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        String schematicDifficulty = df.format(player.schematicDifficulty);

        sendUpdate("""
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
        try {
            LoParkour.log("Connecting to MySQL...");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver");
            }

            connection = DriverManager.getConnection(
                    ("jdbc:mysql://%s:%d/%s?allowPublicKeyRetrieval=true" +
                     "&useSSL=false&useUnicode=true&characterEncoding=utf-8" +
                     "&autoReconnect=true&maxReconnects=2&connectTimeout=5000&socketTimeout=5000")
                            .formatted(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB),
                    Option.SQL_USERNAME, Option.SQL_PASSWORD);

            sendUpdate("CREATE DATABASE IF NOT EXISTS `%s`;".formatted(Option.SQL_DB));
            sendUpdate("USE `%s`;".formatted(Option.SQL_DB));

            // Base options table
            sendUpdate(("CREATE TABLE IF NOT EXISTS `%soptions` " +
                        "(`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(32), `blockLead` INT, " +
                        "`useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useSpecial` BOOLEAN, " +
                        "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) " +
                        "ENGINE = InnoDB CHARSET = utf8;").formatted(Option.SQL_PREFIX));

            // Migrations
            sendUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `time`;".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` ADD `selectedTime` INT NOT NULL;".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` ADD `collectedRewards` MEDIUMTEXT;".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` ADD `locale` VARCHAR(8);".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` ADD `schematicDifficulty` DOUBLE;".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` ADD `sound` BOOLEAN;".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useDifficulty`;".formatted(Option.SQL_PREFIX));
            sendUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useStructure`;".formatted(Option.SQL_PREFIX));

            // Now flush all pending table creations (leaderboard tables queued before connection was ready)
            for (String sql : pendingTableCreations) {
                sendUpdate(sql);
            }
            pendingTableCreations.clear();

            LoParkour.log("Connected to MySQL");
            connected = true;

            // Run deferred callbacks (e.g. initial leaderboard reads)
            for (Runnable cb : onConnectCallbacks) {
                try { cb.run(); } catch (Exception e) {
                    LoParkour.getPlugin().getLogger().severe("Error in SQL onConnect callback: " + e.getMessage());
                }
            }
            onConnectCallbacks.clear();
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().severe("Could not connect to MySQL - check your SQL settings - " + ex.getMessage());
            LoParkour.getPlugin().getLogger().severe("Disabling SQL storage, using local storage instead");
            Option.SQL = false;
        }
    }

    private static void validateConnection() {
        try {
            if (connection == null || !connection.isValid(2)) {
                LoParkour.getPlugin().getLogger().warning("MySQL connection lost, attempting reconnect...");
                connect();
            }
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().severe("Error reconnecting to MySQL - " + ex.getMessage());
            Option.SQL = false;
        }
    }

    /** Returns a PreparedStatement with proper connection validation. Caller must close it. */
    private static PreparedStatement prepareStatement(String sql) {
        validateConnection();
        if (connection == null) return null;
        try {
            return connection.prepareStatement(sql);
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe("Error preparing statement: %s - %s".formatted(sql, ex.getMessage()));
            return null;
        }
    }

    private static void sendUpdate(String sql) {
        validateConnection();
        if (connection == null) return;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe("Error while sending update: %s - %s".formatted(sql, ex.getMessage()));
        }
    }

    private static void sendUpdateSuppressed(String sql) {
        validateConnection();
        if (connection == null) return;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
