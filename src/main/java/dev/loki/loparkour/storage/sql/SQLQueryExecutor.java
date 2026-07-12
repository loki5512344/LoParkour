package dev.loki.loparkour.storage.sql;

import dev.loki.loparkour.LoParkour;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Executes SQL queries with error handling.
 *
 * <p>All public methods accept parameters separately so callers never
 * interpolate user data into SQL strings — preventing SQL injection.</p>
 *
 * @since 5.0.0
 */
class SQLQueryExecutor {

    private final SQLConnectionManager connectionManager;

    public SQLQueryExecutor(SQLConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Executes a DDL/static update with no user-supplied parameters.
     * Only used for CREATE TABLE statements where all values are hardcoded.
     */
    public void executeStaticUpdate(String sql) {
        executeStaticUpdate(sql, false);
    }

    /**
     * Same as {@link #executeStaticUpdate(String)} but optionally suppresses errors.
     * Used for ALTER TABLE migrations that may already have been applied.
     */
    public void executeStaticUpdateSuppressed(String sql) {
        executeStaticUpdate(sql, true);
    }

    private void executeStaticUpdate(String sql, boolean suppressErrors) {
        connectionManager.validateConnection();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (stmt != null) {
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            if (!suppressErrors) {
                LoParkour.getPlugin().getLogger().severe(
                    "Error while executing static update - " + ex.getMessage());
            }
        }
    }

    /**
     * Executes a parameterised update. Values are bound via {@link PreparedStatement#setObject}
     * — the JDBC driver handles escaping, no interpolation ever touches the SQL string.
     *
     * @param sql    SQL with {@code ?} placeholders
     * @param params Values to bind in order
     */
    public void executeUpdate(String sql, Object... params) {
        connectionManager.validateConnection();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (stmt == null) {
                return;
            }
            bindParams(stmt, params);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                "Error while executing update - " + ex.getMessage());
        }
    }

    /**
     * Prepares a statement for callers that need to read a {@link java.sql.ResultSet}.
     * Caller is responsible for closing both the PreparedStatement AND the Connection.
     *
     * @deprecated Use try-with-resources with getConnection() instead to avoid connection leaks
     */
    @Deprecated
    public PreparedStatement prepareStatement(String sql) {
        try {
            return connectionManager.getConnection().prepareStatement(sql);
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                "Error preparing statement: " + ex.getMessage());
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void bindParams(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}
