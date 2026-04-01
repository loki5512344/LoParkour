package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.util.Materials;
import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.style.RandomStyle;
import dev.loki.loparkour.style.Style;
import dev.loki.loparkour.util.ParticleData;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.function.BiFunction;

/**
 * All config-derived constants used at runtime.
 */
public class Option {

    // ── General ───────────────────────────────────────────────────────────────

    public static double BORDER_SIZE;
    public static List<Integer> POSSIBLE_LEADS;
    public static BlockFace HEADING;

    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, String>  OPTIONS_DEFAULTS;

    public static Location GO_BACK_LOC;

    // ── SQL ───────────────────────────────────────────────────────────────────

    public static boolean SQL;
    public static String  SQL_URL;
    public static int     SQL_PORT;
    public static String  SQL_DB;
    public static String  SQL_USERNAME;
    public static String  SQL_PASSWORD;
    public static String  SQL_PREFIX;

    // ── Particles / Sound ─────────────────────────────────────────────────────

    public static ParticleShape   PARTICLE_SHAPE;
    public static Sound           SOUND_TYPE;
    public static int             SOUND_PITCH;
    public static int             SOUND_VOLUME;
    public static Particle        PARTICLE_TYPE;
    public static ParticleData<?> PARTICLE_DATA;

    // ── Generation ────────────────────────────────────────────────────────────

    public static double TYPE_NORMAL, TYPE_SPECIAL, TYPE_SCHEMATICS;
    public static double SPECIAL_ICE, SPECIAL_SLAB, SPECIAL_PANE, SPECIAL_FENCE;
    public static double NORMAL_DISTANCE_1, NORMAL_DISTANCE_2, NORMAL_DISTANCE_3, NORMAL_DISTANCE_4;
    public static double NORMAL_HEIGHT_1, NORMAL_HEIGHT_0, NORMAL_HEIGHT_NEG1, NORMAL_HEIGHT_NEG2;
    public static int    MAX_Y, MIN_Y;
    public static int    BLOCK_CLEANUP_DISTANCE, CLEANUP_INTERVAL;
    /** Block indices to keep behind the rearmost player before trail air-cleanup. */
    public static int    TRAIL_KEEP_BEHIND;
    public static boolean GHOST_MODE_ENABLED;
    public static int     GHOST_SHOW_TOP;
    public static double  GHOST_TRANSPARENCY;

    // ── Jump validation ───────────────────────────────────────────────────────

    public static boolean JUMP_VALIDATION_ENABLED;
    public static double  MAX_JUMP_DISTANCE;
    public static double  MAX_HORIZONTAL_DISTANCE;
    public static double  MAX_VERTICAL_UP;
    public static double  MAX_VERTICAL_DOWN;

    // ── Jump types ────────────────────────────────────────────────────────────

    public static boolean                    JUMP_TYPES_ENABLED;
    public static Map<String, Boolean>       JUMP_TYPE_ENABLED;
    public static Map<String, Double>        JUMP_TYPE_CHANCE;

    // ── init ──────────────────────────────────────────────────────────────────

    public static void init(boolean firstLoad) {
        initSql();
        initParticles();
        initGeneration();
        initGeneral(firstLoad);
        initOptions();

        initStyles("styles.list", Config.CONFIG.fileConfiguration, RandomStyle::new)
            .forEach(Registry::register);
    }

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static void initSql() {
        SQL          = Config.CONFIG.getBoolean("sql.enabled");
        SQL_PORT     = Config.CONFIG.getInt("sql.port");
        SQL_DB       = Config.CONFIG.getString("sql.database");
        SQL_URL      = Config.CONFIG.getString("sql.url");
        SQL_USERNAME = Config.CONFIG.getString("sql.username");
        String envPassword = System.getenv("LOPARKOUR_SQL_PASSWORD");
        SQL_PASSWORD = (envPassword != null && !envPassword.isEmpty())
                ? envPassword
                : Config.CONFIG.getString("sql.password");
        SQL_PREFIX   = Config.CONFIG.getString("sql.prefix");
    }

    // ── Particles / Sound ─────────────────────────────────────────────────────

    private static void initParticles() {
        SOUND_TYPE    = resolveSound(Config.CONFIG.getString("particles.sound-type"));
        PARTICLE_TYPE = resolveParticle(Config.CONFIG.getString("particles.particle-type"));
        PARTICLE_SHAPE = resolveShape(Config.CONFIG.getString("particles.particle-shape"));

        SOUND_PITCH   = Config.CONFIG.getInt("particles.sound-pitch");
        SOUND_VOLUME  = Config.CONFIG.getInt("particles.sound-volume");
        PARTICLE_DATA = new ParticleData<>(PARTICLE_TYPE, null, 10);
    }

    private static Sound resolveSound(String name) {
        if (name != null && !name.isEmpty()) {
            try { return Sound.valueOf(name.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        for (String fb : new String[]{"BLOCK_NOTE_BLOCK_GUITAR", "BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_PLING"}) {
            try { return Sound.valueOf(fb); }
            catch (IllegalArgumentException ignored) {}
        }
        LoParkour.getPlugin().getLogger().warning("Could not resolve any sound, using first available.");
        return Sound.values()[0];
    }

    private static Particle resolveParticle(String name) {
        if (name != null && !name.isEmpty()) {
            try { return Particle.valueOf(name.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        for (String fb : new String[]{"INSTANT_EFFECT", "SPELL_INSTANT", "CRIT"}) {
            try { return Particle.valueOf(fb); }
            catch (IllegalArgumentException ignored) {}
        }
        LoParkour.getPlugin().getLogger().warning("Could not resolve any particle, using first available.");
        return Particle.values()[0];
    }

    private static ParticleShape resolveShape(String name) {
        if (name != null && !name.isEmpty()) {
            try { return ParticleShape.valueOf(name.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return ParticleShape.BOX;
    }

    // ── Generation ────────────────────────────────────────────────────────────

    private static void initGeneration() {
        JUMP_VALIDATION_ENABLED = Config.CONFIG.isPath("jump-validation.enabled") && Config.CONFIG.getBoolean("jump-validation.enabled");
        MAX_JUMP_DISTANCE       = Config.CONFIG.isPath("jump-validation.max-distance")      ? Config.CONFIG.getDouble("jump-validation.max-distance")      : 5.0;
        MAX_HORIZONTAL_DISTANCE = Config.CONFIG.isPath("jump-validation.max-horizontal")    ? Config.CONFIG.getDouble("jump-validation.max-horizontal")    : 4.0;
        MAX_VERTICAL_UP         = Config.CONFIG.isPath("jump-validation.max-vertical-up")   ? Config.CONFIG.getDouble("jump-validation.max-vertical-up")   : 1.0;
        MAX_VERTICAL_DOWN       = Config.CONFIG.isPath("jump-validation.max-vertical-down") ? Config.CONFIG.getDouble("jump-validation.max-vertical-down") : 3.0;

        JUMP_TYPES_ENABLED = Config.CONFIG.isPath("jump-types.enabled") && Config.CONFIG.getBoolean("jump-types.enabled");
        JUMP_TYPE_ENABLED  = new HashMap<>();
        JUMP_TYPE_CHANCE   = new HashMap<>();
        if (Config.CONFIG.isPath("jump-types.types")) {
            for (String type : Config.CONFIG.getChildren("jump-types.types")) {
                String p = "jump-types.types." + type;
                JUMP_TYPE_ENABLED.put(type, Config.CONFIG.isPath(p + ".enabled") && Config.CONFIG.getBoolean(p + ".enabled"));
                JUMP_TYPE_CHANCE.put(type,  Config.CONFIG.isPath(p + ".chance")  ? Config.CONFIG.getDouble(p + ".chance") : 0.0);
            }
        }

        BLOCK_CLEANUP_DISTANCE = Config.CONFIG.isPath("memory.block-cleanup-distance") ? Config.CONFIG.getInt("memory.block-cleanup-distance") : 100;
        CLEANUP_INTERVAL       = Config.CONFIG.isPath("memory.cleanup-interval")       ? Config.CONFIG.getInt("memory.cleanup-interval")       : 100;
        TRAIL_KEEP_BEHIND      = Config.CONFIG.isPath("memory.trail-keep-behind")      ? Config.CONFIG.getInt("memory.trail-keep-behind")      : 10;

        GHOST_MODE_ENABLED = Config.CONFIG.isPath("ghost-mode.enabled")     && Config.CONFIG.getBoolean("ghost-mode.enabled");
        GHOST_SHOW_TOP     = Config.CONFIG.isPath("ghost-mode.show-top")     ? Config.CONFIG.getInt("ghost-mode.show-top")     : 3;
        GHOST_TRANSPARENCY = Config.CONFIG.isPath("ghost-mode.transparency") ? Config.CONFIG.getDouble("ghost-mode.transparency") : 0.5;

        TYPE_NORMAL     = Config.GENERATION.getInt("generation.type.normal")    / 100.0;
        TYPE_SPECIAL    = Config.GENERATION.getInt("generation.type.special")   / 100.0;
        TYPE_SCHEMATICS = Config.GENERATION.getInt("generation.type.schematic") / 100.0;

        SPECIAL_ICE   = Config.GENERATION.getInt("generation.special.ice")   / 100.0;
        SPECIAL_SLAB  = Config.GENERATION.getInt("generation.special.slab")  / 100.0;
        SPECIAL_PANE  = Config.GENERATION.getInt("generation.special.pane")  / 100.0;
        SPECIAL_FENCE = Config.GENERATION.getInt("generation.special.fence") / 100.0;

        NORMAL_DISTANCE_1 = Config.GENERATION.getInt("generation.normal.distance.1") / 100.0;
        NORMAL_DISTANCE_2 = Config.GENERATION.getInt("generation.normal.distance.2") / 100.0;
        NORMAL_DISTANCE_3 = Config.GENERATION.getInt("generation.normal.distance.3") / 100.0;
        NORMAL_DISTANCE_4 = Config.GENERATION.getInt("generation.normal.distance.4") / 100.0;

        NORMAL_HEIGHT_1    = Config.GENERATION.getInt("generation.normal.height.1")  / 100.0;
        NORMAL_HEIGHT_0    = Config.GENERATION.getInt("generation.normal.height.0")  / 100.0;
        NORMAL_HEIGHT_NEG1 = Config.GENERATION.getInt("generation.normal.height.-1") / 100.0;
        NORMAL_HEIGHT_NEG2 = Config.GENERATION.getInt("generation.normal.height.-2") / 100.0;

        MAX_Y = Config.GENERATION.getInt("generation.settings.max-y");
        MIN_Y = Config.GENERATION.getInt("generation.settings.min-y");

        if (MIN_Y >= MAX_Y) {
            MIN_Y = 100; MAX_Y = 200;
            LoParkour.getPlugin().getLogger().severe("min-y >= max-y in generation.yml — using defaults 100/200.");
        }
    }

    // ── General ───────────────────────────────────────────────────────────────

    private static void initGeneral(boolean firstLoad) {
        GO_BACK_LOC = parseLocation(Config.CONFIG.getString("bungeecord.go-back"));
        try {
            String axesRaw = Config.CONFIG.getString("bungeecord.go-back-axes");
            if (!axesRaw.isEmpty()) {
                String[] axes = axesRaw.split(",");
                if (axes.length >= 2) {
                    GO_BACK_LOC.setPitch(Float.parseFloat(axes[0].trim()));
                    GO_BACK_LOC.setYaw(Float.parseFloat(axes[1].trim()));
                }
            }
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().warning("Invalid bungeecord.go-back-axes, using 0,0");
        }

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
        try {
            String[] values = location.replaceAll("[()]", "").replaceAll("[, ]", " ").trim().split("\\s+");
            if (values.length < 4) {
                throw new IllegalArgumentException("Expected 4 values (x,y,z,world), got " + values.length);
            }
            World world = Bukkit.getWorld(values[3]);
            if (world == null) world = Bukkit.getWorlds().get(0);
            return new Location(world, Double.parseDouble(values[0]),
                    Double.parseDouble(values[1]), Double.parseDouble(values[2]));
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().warning(
                    "Invalid bungeecord.go-back value '" + location + "'. Falling back to spawn world origin.");
            World fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            return new Location(fallback, 0, 100, 0);
        }
    }

    public static Set<Style> initStyles(String path, FileConfiguration config, BiFunction<String, List<Material>, Style> fn) {
        var styles = new HashSet<Style>();
        ConfigAccessor accessor = new ConfigAccessor(config, "config.yml");
        for (String style : accessor.getChildren(path, false)) {
            styles.add(fn.apply(style,
                config.getStringList("%s.%s".formatted(path, style)).stream()
                    .map(name -> {
                        Material m = Materials.parse(name);
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
