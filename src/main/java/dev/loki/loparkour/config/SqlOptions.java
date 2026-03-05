package dev.loki.loparkour.config;

/**
 * SQL connection settings. Populated by {@link Option#init}.
 */
public final class SqlOptions {

    public static boolean SQL;
    public static int     PORT;
    public static String  URL;
    public static String  DB;
    public static String  USERNAME;
    public static String  PASSWORD;
    public static String  PREFIX;

    static void init() {
        SQL      = Config.CONFIG.getBoolean("sql.enabled");
        PORT     = Config.CONFIG.getInt("sql.port");
        DB       = Config.CONFIG.getString("sql.database");
        URL      = Config.CONFIG.getString("sql.url");
        USERNAME = Config.CONFIG.getString("sql.username");
        PASSWORD = Config.CONFIG.getString("sql.password");
        PREFIX   = Config.CONFIG.getString("sql.prefix");
    }

    private SqlOptions() {}
}
