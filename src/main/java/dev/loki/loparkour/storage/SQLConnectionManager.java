package dev.loki.loparkour.storage;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Option;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SQL database connection lifecycle.
 *
 * @since 5.0.0
 */
class SQLConnectionManager {

    private Connection connection;
    private volatile boolean connected = false;
    private final List<Runnable> onConnectCallbacks = new CopyOnWriteArrayList<>();

    public boolean isConnected() {
        return connected;
    }

    public void runWhenConnected(Runnable callback) {
        if (connected) {
            callback.run();
        } else {
            onConnectCallbacks.add(callback);
        }
    }

    public void connect() {
        try {
            LoParkour.log("Connecting to MySQL...");
            loadDriver();

            connection = DriverManager.getConnection(
                    buildConnectionUrl(),
                    Option.SQL_USERNAME,
                    Option.SQL_PASSWORD);

            connected = true;
            LoParkour.log("Connected to MySQL");

            executeCallbacks();
        } catch (Exception ex) {
            handleConnectionError(ex);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LoParkour.log("Closed connection to MySQL");
            }
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while trying to close connection to SQL database - " + ex.getMessage());
        }
    }

    public PreparedStatement prepareStatement(String sql) {
        validateConnection();
        if (connection == null) return null;

        try {
            return connection.prepareStatement(sql);
        } catch (SQLException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error preparing statement: %s - %s".formatted(sql, ex.getMessage()));
            return null;
        }
    }

    public void validateConnection() {
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

    private void loadDriver() throws ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            Class.forName("com.mysql.jdbc.Driver");
        }
    }

    private String buildConnectionUrl() {
        return ("jdbc:mysql://%s:%d/%s?allowPublicKeyRetrieval=true" +
                "&useSSL=false&useUnicode=true&characterEncoding=utf-8" +
                "&autoReconnect=true&maxReconnects=2&connectTimeout=5000&socketTimeout=5000")
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
    }
}
