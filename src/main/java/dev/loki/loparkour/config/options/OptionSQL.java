package dev.loki.loparkour.config.options;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;

/**
 * SQL database configuration options.
 */
public class OptionSQL {

    public static boolean SQL;
    public static String  SQL_URL;
    public static int     SQL_PORT;
    public static String  SQL_DB;
    public static String  SQL_USERNAME;
    public static String  SQL_PASSWORD;
    public static String  SQL_PREFIX;

    public static void init() {
        SQL          = Config.CONFIG.getBoolean("sql.enabled");
        SQL_PORT     = Config.CONFIG.getInt("sql.port");
        SQL_DB       = Config.CONFIG.getString("sql.database");
        SQL_URL      = Config.CONFIG.getString("sql.url");
        SQL_USERNAME = Config.CONFIG.getString("sql.username");

        // Environment variable override for password
        String envPassword = System.getenv("LOPARKOUR_SQL_PASSWORD");
        SQL_PASSWORD = (envPassword != null && !envPassword.isEmpty())
                ? envPassword
                : Config.CONFIG.getString("sql.password");

        SQL_PREFIX   = Config.CONFIG.getString("sql.prefix");
    }
}
