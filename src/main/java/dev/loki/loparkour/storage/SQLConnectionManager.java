package dev.loki.loparkour.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Option;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MySQL access via HikariCP pool.
 */
class SQLConnectionManager {

    private HikariDataSource dataSource;
    private volatile boolean connected = false;
    private final List<Runnable> onConnectCallbacks = new CopyOnWriteArrayList<>();

    public boolean isConnected() {
        return connected && dataSource != null && !dataSource.isClosed();
    }

    public void runWhenConnected(Runnable callback) {
        if (isConnected()) {
            callback.run();
        } else {
            onConnectCallbacks.add(callback);
        }
    }

    public void connect() {
        try {
            LoParkour.log("Connecting to MySQL (pool)...");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(buildConnectionUrl());
            config.setUsername(Option.SQL_USERNAME);
            config.setPassword(Option.SQL_PASSWORD);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(5000);
            config.setMaxLifetime(1_800_000);
            config.setPoolName("LoParkour");

            dataSource = new HikariDataSource(config);
            connected = true;
            LoParkour.log("Connected to MySQL");

            executeCallbacks();
        } catch (Exception ex) {
            handleConnectionError(ex);
        }
    }

    public void close() {
        connected = false;
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LoParkour.log("Closed MySQL pool");
        }
        dataSource = null;
    }

    /**
     * Get a connection from the pool.
     * Caller MUST close the connection when done (use try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Database not connected");
        }
        return dataSource.getConnection();
    }

    /**
     * Ensures the pool was created; does not recreate the pool on failure (restart required).
     */
    public void validateConnection() {
        if (!isConnected()) {
            LoParkour.getPlugin().getLogger().warning("MySQL pool not available");
            Option.SQL = false;
        }
    }

    private String buildConnectionUrl() {
        return ("jdbc:mysql://%s:%d/%s?allowPublicKeyRetrieval=true"
                + "&useSSL=false&useUnicode=true&characterEncoding=utf-8"
                + "&autoReconnect=true&maxReconnects=2&connectTimeout=5000&socketTimeout=5000")
                .formatted(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB);
    }

    private void executeCallbacks() {
        for (Runnable cb : onConnectCallbacks) {
            try {
                cb.run();
            } catch (Exception e) {
                LoParkour.getPlugin().getLogger().severe("Error in SQL onConnect callback: " + e.getMessage());
            }
        }
        onConnectCallbacks.clear();
    }

    private void handleConnectionError(Exception ex) {
        LoParkour.getPlugin().getLogger().severe(
                "Could not connect to MySQL - check your SQL settings - " + ex.getMessage());
        LoParkour.getPlugin().getLogger().severe("Disabling SQL storage, using local storage instead");
        Option.SQL = false;
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
        connected = false;
    }
}
