package dev.loki.loparkour.storage.sql;

import dev.loki.loparkour.config.options.Option;

/**
 * Manages database schema migrations.
 *
 * @since 5.0.0
 */
class SQLMigrationManager {

    private final SQLQueryExecutor queryExecutor;

    SQLMigrationManager(SQLQueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public void initializeDatabase() {
        createDatabase();
        createOptionsTable();
        runMigrations();
    }

    private void createDatabase() {
        queryExecutor.executeStaticUpdate("CREATE DATABASE IF NOT EXISTS `%s`;".formatted(Option.SQL_DB));
        queryExecutor.executeStaticUpdate("USE `%s`;".formatted(Option.SQL_DB));
    }

    private void createOptionsTable() {
        queryExecutor.executeStaticUpdate(
                ("CREATE TABLE IF NOT EXISTS `%soptions` " +
                 "(`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(32), `blockLead` INT, " +
                 "`useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useSpecial` BOOLEAN, " +
                 "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) " +
                 "ENGINE = InnoDB CHARSET = utf8;").formatted(Option.SQL_PREFIX));
    }

    private void runMigrations() {
        String prefix = Option.SQL_PREFIX;

        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `time`;".formatted(prefix));
        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` ADD `selectedTime` INT NOT NULL;".formatted(prefix));
        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` ADD `collectedRewards` MEDIUMTEXT;".formatted(prefix));
        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` ADD `locale` VARCHAR(8);".formatted(prefix));
        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` ADD `schematicDifficulty` DOUBLE;".formatted(prefix));
        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` ADD `sound` BOOLEAN;".formatted(prefix));
        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useDifficulty`;".formatted(prefix));
        queryExecutor.executeStaticUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useStructure`;".formatted(prefix));

        // Fix legacy Boolean locale values (true/false) to proper locale codes
        queryExecutor.executeStaticUpdateSuppressed(
            "UPDATE `%soptions` SET `locale` = 'en' WHERE `locale` IN ('true', 'false', '1', '0');".formatted(prefix));

        createAdaptiveStatsTable();
    }

    private void createAdaptiveStatsTable() {
        queryExecutor.executeStaticUpdate(
                ("CREATE TABLE IF NOT EXISTS loparkour_player_stats ("
                 + "player_uuid VARCHAR(36) PRIMARY KEY, "
                 + "skill_rating DOUBLE DEFAULT 1.0, "
                 + "sessions_count INT DEFAULT 0, "
                 + "total_jumps INT DEFAULT 0, "
                 + "total_falls INT DEFAULT 0, "
                 + "longest_streak INT DEFAULT 0, "
                 + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                 + "INDEX idx_skill_rating (skill_rating), "
                 + "INDEX idx_last_updated (last_updated)"
                 + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"));
    }
}
