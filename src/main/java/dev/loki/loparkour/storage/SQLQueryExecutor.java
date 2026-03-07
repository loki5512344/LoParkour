package dev.loki.loparkour.storage;

import dev.loki.loparkour.LoParkour;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Executes SQL queries with error handling.
 *
 * @since 5.0.0
 */
class SQLQueryExecutor {

    private final SQLConnectionManager connectionManager;

    public SQLQueryExecutor(SQLConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void executeUpdate(String sql) {
        executeUpdate(sql, false);
    }

    public void executeUpdateSuppressed(String sql) {
        executeUpdate(sql, true);
    }

    private void executeUpdate(String sql, boolean suppressErrors) {
        connectionManager.validateConnection();

        try (PreparedStatement stmt = connectionManager.prepareStatement(sql)) {
            if (stmt != null) {
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            if (!suppressErrors) {
                LoParkour.getPlugin().getLogger().severe(
                        "Error while sending update: %s - %s".formatted(sql, ex.getMessage()));
            }
        }
    }

    public PreparedStatement prepareStatement(String sql) {
        return connectionManager.prepareStatement(sql);
    }
}
