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
 * <p><b>Security:</b> Every query that touches user-supplied data uses
 * {@code ?} placeholders bound via {@link PreparedStatement#setObject}.
 * No string interpolation of player names, UUIDs, scores, or any other
 * external value ever reaches the SQL string itself.</p>
 *
 * @since 5.0.0
 */
class StorageSQL {

    private static final SQLConnectionManager connectionManager = new SQLConnectionManager();
    private static final SQLQueryExecutor     queryExecutor     = new SQLQueryExecutor(connectionManager);
    private static final SQLMigrationManager  migrationManager  = new SQLMigrationManager(queryExecutor);

    private static boolean initialized = false;
    private static final List<String> pendingTableCreations = new CopyOnWriteArrayList<>();

    // ── Connection lifecycle ──────────────────────────────────────────────────

    public static boolean isConnected() {
        return connectionManager.isConnected();
    }

    public static void runWhenConnected(Runnable callback) {
        connectionManager.runWhenConnected(callback);
    }

    public static void close() {
        connectionManager.close();
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Queues creation of the leaderboard table for {@code mode}.
     * Table name is constructed from a config-defined prefix (admin-controlled,
     * not user input) so formatting here is safe.
     */
    public static void init(String mode) {
        // Table names are derived from admin config, not player input — safe to format.
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS `%s`
                (
                    uuid       CHAR(36)    NOT NULL PRIMARY KEY,
                    name       VARCHAR(16),
                    time       VARCHAR(16),
                    difficulty VARCHAR(3),
                    score      INT
                ) CHARSET = utf8 ENGINE = InnoDB;
                """.formatted(getTableName(mode));

        if (!initialized) {
            initialized = true;
            pendingTableCreations.add(createTableSql);
            Bukkit.getScheduler().runTaskAsynchronously(LoParkour.getPlugin(), StorageSQL::connect);
        } else {
            pendingTableCreations.add(createTableSql);
        }
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        // Table name from admin config — safe to format; no user data here.
        String sql = "SELECT * FROM `%s`;".formatted(getTableName(mode));
        try (PreparedStatement stmt = queryExecutor.prepareStatement(sql)) {
            if (stmt == null) return new HashMap<>();
            try (ResultSet rs = stmt.executeQuery()) {
                Map<UUID, Score> scores = new HashMap<>();
                while (rs.next()) {
                    scores.put(
                        UUID.fromString(rs.getString("uuid")),
                        new Score(
                            rs.getString("name"),
                            rs.getString("time"),
                            rs.getString("difficulty"),
                            rs.getInt("score")));
                }
                return scores;
            }
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                "Error reading leaderboard for mode %s - %s".formatted(mode, ex.getMessage()));
            return new HashMap<>();
        }
    }

    /**
     * Writes all scores for a mode.
     *
     * <p>Uses an INSERT … ON DUPLICATE KEY UPDATE with {@code ?} placeholders.
     * Player name, time, difficulty come from the leaderboard record — all bound
     * as parameters, never interpolated.</p>
     */
    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        // Table name is from admin config — safe to format.
        String sql = """
                INSERT INTO `%s` (uuid, name, time, difficulty, score)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name       = VALUES(name),
                    time       = VALUES(time),
                    difficulty = VALUES(difficulty),
                    score      = VALUES(score);
                """.formatted(getTableName(mode));

        new HashMap<>(scores).forEach((uuid, score) ->
            queryExecutor.executeUpdate(sql,
                uuid.toString(),
                score.name(),
                score.time(),
                score.difficulty(),
                score.score()));
    }

    // ── Player settings ───────────────────────────────────────────────────────

    /**
     * Reads player settings from the options table.
     *
     * <p>UUID is bound as a parameter — never interpolated into the query string.</p>
     */
    public static void readPlayer(@NotNull ParkourPlayer player) {
        // Table name from admin config — safe. UUID bound as parameter below.
        String sql = "SELECT * FROM `%soptions` WHERE uuid = ?;".formatted(Option.SQL_PREFIX);

        try (PreparedStatement stmt = queryExecutor.prepareStatement(sql)) {
            if (stmt == null) {
                player.setSettings(new HashMap<>());
                return;
            }

            // Bind UUID as parameter — no injection possible
            stmt.setString(1, player.getUUID().toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    player.setSettings(new HashMap<>());
                    return;
                }

                Map<String, Object> settings = new HashMap<>();
                for (String key : ParkourPlayer.PLAYER_COLUMNS.keySet()) {
                    try {
                        settings.put(key, rs.getObject(key));
                    } catch (SQLException ex) {
                        LoParkour.getPlugin().getLogger().severe(
                            "Error reading SQL column %s for %s - %s"
                                .formatted(key, player.getName(), ex.getMessage()));
                    }
                }
                player.setSettings(settings);
            }
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                "Error reading player data for %s - %s".formatted(player.getName(), ex.getMessage()));
        }
    }

    /**
     * Writes player settings to the options table.
     *
     * <p>All 12 player-controlled values (style, locale, rewards, etc.) are bound
     * as {@code ?} parameters. None of them are interpolated into the SQL string.</p>
     */
    public static void writePlayer(@NotNull ParkourPlayer player) {
        DecimalFormat df = new DecimalFormat("#.######",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        double schemDiff = df.format(player.schematicDifficulty) != null
            ? player.schematicDifficulty : 0.0;

        // Table name from admin config — safe to format.
        String sql = """
                INSERT INTO `%soptions`
                    (uuid, style, blockLead, useParticles, useSpecial, showFallMsg,
                     showScoreboard, selectedTime, collectedRewards, locale,
                     schematicDifficulty, sound)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    style               = VALUES(style),
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

        queryExecutor.executeUpdate(sql,
            player.getUUID().toString(),        // 1  uuid
            player.style,                       // 2  style
            player.blockLead,                   // 3  blockLead
            player.particles,                   // 4  useParticles
            player.useSpecialBlocks,            // 5  useSpecial
            player.showFallMessage,             // 6  showFallMsg
            player.showScoreboard,              // 7  showScoreboard
            player.selectedTime,                // 8  selectedTime
            String.join(",", player.collectedRewards), // 9  collectedRewards
            player.locale,                      // 10 locale
            schemDiff,                          // 11 schematicDifficulty
            player.sound                        // 12 sound
        );
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /**
     * Table name is built from the admin-configured SQL prefix and the internal
     * mode name (e.g. "default", "speedrun"). Neither is user-supplied.
     */
    private static String getTableName(String mode) {
        return "%sleaderboard-%s".formatted(Option.SQL_PREFIX, mode);
    }

    private static void connect() {
        connectionManager.connect();
        migrationManager.initializeDatabase();

        for (String sql : pendingTableCreations) {
            queryExecutor.executeStaticUpdate(sql);
        }
        pendingTableCreations.clear();
    }
}
