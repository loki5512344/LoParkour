package dev.loki.loparkour.storage;

import dev.loki.loparkour.config.Option;

/**
 * Manages database schema migrations.
 *
 * @since 5.0.0
 */
class SQLMigrationManager {

    private final SQLQueryExecutor queryExecutor;

    public SQLMigrationManager(SQLQueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public void initializeDatabase() {
        createDatabase();
        createOptionsTable();
        runMigrations();
    }

    private void createDatabase() {
        queryExecutor.executeUpdate("CREATE DATABASE IF NOT EXISTS `%s`;".formatted(Option.SQL_DB));
        queryExecutor.executeUpdate("USE `%s`;".formatted(Option.SQL_DB));
    }

    private void createOptionsTable() {
        queryExecutor.executeUpdate(
                ("CREATE TABLE IF NOT EXISTS `%soptions` " +
                 "(`uuid` CHAR(36) NOT NULL, `time` VARCHAR(8), `style` VARCHAR(32), `blockLead` INT, " +
                 "`useParticles` BOOLEAN, `useDifficulty` BOOLEAN, `useSpecial` BOOLEAN, " +
                 "`showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) " +
                 "ENGINE = InnoDB CHARSET = utf8;").formatted(Option.SQL_PREFIX));
    }

    private void runMigrations() {
        String prefix = Option.SQL_PREFIX;

        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `time`;".formatted(prefix));
        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` ADD `selectedTime` INT NOT NULL;".formatted(prefix));
        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` ADD `collectedRewards` MEDIUMTEXT;".formatted(prefix));
        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` ADD `locale` VARCHAR(8);".formatted(prefix));
        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` ADD `schematicDifficulty` DOUBLE;".formatted(prefix));
        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` ADD `sound` BOOLEAN;".formatted(prefix));
        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useDifficulty`;".formatted(prefix));
        queryExecutor.executeUpdateSuppressed("ALTER TABLE `%soptions` DROP COLUMN `useStructure`;".formatted(prefix));
    }
}
