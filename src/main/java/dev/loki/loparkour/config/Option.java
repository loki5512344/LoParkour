package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.style.RandomStyle;
import dev.loki.loparkour.style.Style;
import dev.loki.loparkour.util.ParticleData;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Top-level config constants used throughout the plugin.
 *
 * Heavy sub-sections are delegated to focused classes:
 * <ul>
 *   <li>{@link SqlOptions}         – SQL connection settings</li>
 *   <li>{@link ParticleOptions}    – particle &amp; sound settings</li>
 *   <li>{@link GenerationOptions}  – generation &amp; jump-validation settings</li>
 * </ul>
 *
 * Those classes expose their own static fields. Option re-exports the most
 * commonly accessed ones via forwarding fields so existing call-sites compile
 * without changes.
 */
public class Option {

    // ── General ───────────────────────────────────────────────────────────────

    public static double BORDER_SIZE;
    public static List<Integer> POSSIBLE_LEADS;
    public static BlockFace HEADING;

    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, String>  OPTIONS_DEFAULTS;

    public static Location GO_BACK_LOC;

    // ── Forwarded from sub-classes (backwards compat) ─────────────────────────

    /** @see SqlOptions#SQL */
    public static boolean SQL;
    public static String  SQL_URL;
    public static int     SQL_PORT;
    public static String  SQL_DB;
    public static String  SQL_USERNAME;
    public static String  SQL_PASSWORD;
    public static String  SQL_PREFIX;

    /** @see ParticleOptions#SHAPE */
    public static ParticleShape PARTICLE_SHAPE;
    /** @see ParticleOptions#SOUND_TYPE */
    public static Sound SOUND_TYPE;
    /** @see ParticleOptions#SOUND_PITCH */
    public static int SOUND_PITCH;
    /** @see ParticleOptions#SOUND_VOLUME */
    public static int SOUND_VOLUME;
    /** @see ParticleOptions#PARTICLE_TYPE */
    public static Particle PARTICLE_TYPE;
    /** @see ParticleOptions#PARTICLE_DATA */
    public static ParticleData<?> PARTICLE_DATA;

    /** @see GenerationOptions */
    public static double TYPE_NORMAL, TYPE_SPECIAL, TYPE_SCHEMATICS;
    public static double SPECIAL_ICE, SPECIAL_SLAB, SPECIAL_PANE, SPECIAL_FENCE;
    public static double NORMAL_DISTANCE_1, NORMAL_DISTANCE_2, NORMAL_DISTANCE_3, NORMAL_DISTANCE_4;
    public static double NORMAL_HEIGHT_1, NORMAL_HEIGHT_0, NORMAL_HEIGHT_NEG1, NORMAL_HEIGHT_NEG2;
    public static int    MAX_Y, MIN_Y;
    public static int    BLOCK_CLEANUP_DISTANCE, CLEANUP_INTERVAL;
    public static boolean GHOST_MODE_ENABLED;
    public static int     GHOST_SHOW_TOP;
    public static double  GHOST_TRANSPARENCY;

    // ── init ──────────────────────────────────────────────────────────────────

    public static void init(boolean firstLoad) {
        SqlOptions.init();
        ParticleOptions.init();
        GenerationOptions.init();

        syncForwardedFields();
        initGeneral(firstLoad);
        initOptions();

        initStyles("styles.list", Config.CONFIG.fileConfiguration, RandomStyle::new)
            .forEach(Registry::register);
    }

    /** Copy sub-class fields to this class so existing code still compiles. */
    private static void syncForwardedFields() {
        SQL          = SqlOptions.SQL;
        SQL_URL      = SqlOptions.URL;
        SQL_PORT     = SqlOptions.PORT;
        SQL_DB       = SqlOptions.DB;
        SQL_USERNAME = SqlOptions.USERNAME;
        SQL_PASSWORD = SqlOptions.PASSWORD;
        SQL_PREFIX   = SqlOptions.PREFIX;

        PARTICLE_SHAPE = ParticleOptions.SHAPE;
        SOUND_TYPE     = ParticleOptions.SOUND_TYPE;
        SOUND_PITCH    = ParticleOptions.SOUND_PITCH;
        SOUND_VOLUME   = ParticleOptions.SOUND_VOLUME;
        PARTICLE_TYPE  = ParticleOptions.PARTICLE_TYPE;
        PARTICLE_DATA  = ParticleOptions.PARTICLE_DATA;

        TYPE_NORMAL     = GenerationOptions.TYPE_NORMAL;
        TYPE_SPECIAL    = GenerationOptions.TYPE_SPECIAL;
        TYPE_SCHEMATICS = GenerationOptions.TYPE_SCHEMATICS;

        SPECIAL_ICE   = GenerationOptions.SPECIAL_ICE;
        SPECIAL_SLAB  = GenerationOptions.SPECIAL_SLAB;
        SPECIAL_PANE  = GenerationOptions.SPECIAL_PANE;
        SPECIAL_FENCE = GenerationOptions.SPECIAL_FENCE;

        NORMAL_DISTANCE_1 = GenerationOptions.NORMAL_DISTANCE_1;
        NORMAL_DISTANCE_2 = GenerationOptions.NORMAL_DISTANCE_2;
        NORMAL_DISTANCE_3 = GenerationOptions.NORMAL_DISTANCE_3;
        NORMAL_DISTANCE_4 = GenerationOptions.NORMAL_DISTANCE_4;

        NORMAL_HEIGHT_1    = GenerationOptions.NORMAL_HEIGHT_1;
        NORMAL_HEIGHT_0    = GenerationOptions.NORMAL_HEIGHT_0;
        NORMAL_HEIGHT_NEG1 = GenerationOptions.NORMAL_HEIGHT_NEG1;
        NORMAL_HEIGHT_NEG2 = GenerationOptions.NORMAL_HEIGHT_NEG2;

        MAX_Y = GenerationOptions.MAX_Y;
        MIN_Y = GenerationOptions.MIN_Y;

        BLOCK_CLEANUP_DISTANCE = GenerationOptions.BLOCK_CLEANUP_DISTANCE;
        CLEANUP_INTERVAL       = GenerationOptions.CLEANUP_INTERVAL;

        GHOST_MODE_ENABLED = GenerationOptions.GHOST_MODE_ENABLED;
        GHOST_SHOW_TOP     = GenerationOptions.GHOST_SHOW_TOP;
        GHOST_TRANSPARENCY = GenerationOptions.GHOST_TRANSPARENCY;
    }

    // ── General init ──────────────────────────────────────────────────────────

    private static void initGeneral(boolean firstLoad) {
        GO_BACK_LOC = parseLocation(Config.CONFIG.getString("bungeecord.go-back"));
        String[] axes = Config.CONFIG.getString("bungeecord.go-back-axes").split(",");
        GO_BACK_LOC.setPitch(Float.parseFloat(axes[0]));
        GO_BACK_LOC.setYaw(Float.parseFloat(axes[1]));

        POSSIBLE_LEADS = Config.CONFIG.getIntList("options.leads.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1 || lead > 128) {
                LoParkour.getPlugin().getLogger().severe("Invalid lead: %d. Must be 1–128.".formatted(lead));
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }

        String heading = Config.GENERATION.getString("advanced.island.parkour.heading");
        HEADING = switch (heading.toLowerCase()) {
            case "north" -> BlockFace.NORTH;
            case "south" -> BlockFace.SOUTH;
            case "west"  -> BlockFace.WEST;
            case "east"  -> BlockFace.EAST;
            default -> {
                LoParkour.getPlugin().getLogger().severe("Invalid heading: " + heading);
                yield BlockFace.NORTH;
            }
        };

        if (firstLoad) {
            BORDER_SIZE = Config.GENERATION.getDouble("advanced.border-size");
        }
    }

    private static void initOptions() {
        List<ParkourOption> options = new ArrayList<>(Arrays.asList(ParkourOption.values()));
        options.remove(ParkourOption.JOIN);
        options.remove(ParkourOption.ADMIN);

        OPTIONS_DEFAULTS = new HashMap<>();
        OPTIONS_ENABLED  = new HashMap<>();

        for (ParkourOption option : options) {
            String parent = "default-values." + option.path;
            OPTIONS_ENABLED.put(option, Config.CONFIG.getBoolean(parent + ".enabled"));

            if (Config.CONFIG.isPath(parent + ".default")) {
                Object value = Config.CONFIG.get(parent + ".default");
                if (value != null) OPTIONS_DEFAULTS.put(option, String.valueOf(value));
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Location parseLocation(String location) {
        String[] values = location.replaceAll("[()]", "").replaceAll("[, ]", " ").split(" ");
        World world = Bukkit.getWorld(values[3]);
        if (world == null) world = Bukkit.getWorlds().get(0);
        return new Location(world, Double.parseDouble(values[0]),
                Double.parseDouble(values[1]), Double.parseDouble(values[2]));
    }

    public static Set<Style> initStyles(String path, FileConfiguration config, BiFunction<String, List<Material>, Style> fn) {
        var styles = new HashSet<Style>();
        for (String style : Locales.getChildren(config, path, false)) {
            styles.add(fn.apply(style,
                config.getStringList("%s.%s".formatted(path, style)).stream()
                    .map(name -> {
                        Material m = Material.getMaterial(name.toUpperCase());
                        if (m == null) {
                            LoParkour.getPlugin().getLogger().severe("Invalid material %s in style %s".formatted(name, style));
                            return Material.STONE;
                        }
                        return m;
                    })
                    .toList()));
        }
        return styles;
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public enum ParticleShape { DOT, CIRCLE, BOX }
}
