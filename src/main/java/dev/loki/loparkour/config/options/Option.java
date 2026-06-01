package dev.loki.loparkour.config.options;

import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.options.*;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.style.core.RandomStyle;
import dev.loki.loparkour.style.core.Style;
import dev.loki.loparkour.util.particle.ParticleData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Facade for all config-derived constants.
 * Delegates to specialized option classes in config.options package.
 */
public class Option {

    // ── Initialization ────────────────────────────────────────────────────────

    public static void init(boolean firstLoad) {
        OptionSQL.init();
        OptionParticles.init();
        OptionGeneration.init();
        OptionGeneral.init(firstLoad);
        OptionGeneral.initOptions();

        // Sync all fields to Option facade
        syncFields();

        OptionStyles.initStyles("styles.list", Config.CONFIG.fileConfiguration, RandomStyle::new)
            .forEach(Registry::register);
    }

    // ── Direct field re-exports for backward compatibility ───────────────────

    // SQL
    public static boolean SQL;
    public static String SQL_URL;
    public static int SQL_PORT;
    public static String SQL_DB;
    public static String SQL_USERNAME;
    public static String SQL_PASSWORD;
    public static String SQL_PREFIX;

    // Particles
    public static ParticleShape PARTICLE_SHAPE;
    public static Sound SOUND_TYPE;
    public static int SOUND_PITCH;
    public static int SOUND_VOLUME;
    public static Particle PARTICLE_TYPE;
    public static ParticleData<?> PARTICLE_DATA;

    // Generation
    public static double TYPE_NORMAL, TYPE_SPECIAL, TYPE_SCHEMATICS;
    public static double SPECIAL_ICE, SPECIAL_SLAB, SPECIAL_PANE, SPECIAL_FENCE;
    public static double NORMAL_DISTANCE_1, NORMAL_DISTANCE_2, NORMAL_DISTANCE_3, NORMAL_DISTANCE_4;
    public static double NORMAL_HEIGHT_1, NORMAL_HEIGHT_0, NORMAL_HEIGHT_NEG1, NORMAL_HEIGHT_NEG2;
    public static int MAX_Y, MIN_Y;
    public static int BLOCK_CLEANUP_DISTANCE, CLEANUP_INTERVAL, TRAIL_KEEP_BEHIND;
    public static boolean GHOST_MODE_ENABLED;
    public static int GHOST_SHOW_TOP;
    public static double GHOST_TRANSPARENCY;
    public static boolean JUMP_VALIDATION_ENABLED;
    public static double MAX_JUMP_DISTANCE, MAX_HORIZONTAL_DISTANCE, MAX_VERTICAL_UP, MAX_VERTICAL_DOWN;
    public static boolean JUMP_TYPES_ENABLED;
    public static Map<String, Boolean> JUMP_TYPE_ENABLED;
    public static Map<String, Double> JUMP_TYPE_CHANCE;

    // General
    public static double BORDER_SIZE;
    public static List<Integer> POSSIBLE_LEADS;
    public static BlockFace HEADING;
    public static Location GO_BACK_LOC;
    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, String> OPTIONS_DEFAULTS;

    private static void syncFields() {
        // SQL
        SQL = OptionSQL.SQL;
        SQL_URL = OptionSQL.SQL_URL;
        SQL_PORT = OptionSQL.SQL_PORT;
        SQL_DB = OptionSQL.SQL_DB;
        SQL_USERNAME = OptionSQL.SQL_USERNAME;
        SQL_PASSWORD = OptionSQL.SQL_PASSWORD;
        SQL_PREFIX = OptionSQL.SQL_PREFIX;

        // Particles
        PARTICLE_SHAPE = OptionParticles.PARTICLE_SHAPE;
        SOUND_TYPE = OptionParticles.SOUND_TYPE;
        SOUND_PITCH = OptionParticles.SOUND_PITCH;
        SOUND_VOLUME = OptionParticles.SOUND_VOLUME;
        PARTICLE_TYPE = OptionParticles.PARTICLE_TYPE;
        PARTICLE_DATA = OptionParticles.PARTICLE_DATA;

        // Generation
        TYPE_NORMAL = OptionGeneration.TYPE_NORMAL;
        TYPE_SPECIAL = OptionGeneration.TYPE_SPECIAL;
        TYPE_SCHEMATICS = OptionGeneration.TYPE_SCHEMATICS;
        SPECIAL_ICE = OptionGeneration.SPECIAL_ICE;
        SPECIAL_SLAB = OptionGeneration.SPECIAL_SLAB;
        SPECIAL_PANE = OptionGeneration.SPECIAL_PANE;
        SPECIAL_FENCE = OptionGeneration.SPECIAL_FENCE;
        NORMAL_DISTANCE_1 = OptionGeneration.NORMAL_DISTANCE_1;
        NORMAL_DISTANCE_2 = OptionGeneration.NORMAL_DISTANCE_2;
        NORMAL_DISTANCE_3 = OptionGeneration.NORMAL_DISTANCE_3;
        NORMAL_DISTANCE_4 = OptionGeneration.NORMAL_DISTANCE_4;
        NORMAL_HEIGHT_1 = OptionGeneration.NORMAL_HEIGHT_1;
        NORMAL_HEIGHT_0 = OptionGeneration.NORMAL_HEIGHT_0;
        NORMAL_HEIGHT_NEG1 = OptionGeneration.NORMAL_HEIGHT_NEG1;
        NORMAL_HEIGHT_NEG2 = OptionGeneration.NORMAL_HEIGHT_NEG2;
        MAX_Y = OptionGeneration.MAX_Y;
        MIN_Y = OptionGeneration.MIN_Y;
        BLOCK_CLEANUP_DISTANCE = OptionGeneration.BLOCK_CLEANUP_DISTANCE;
        CLEANUP_INTERVAL = OptionGeneration.CLEANUP_INTERVAL;
        TRAIL_KEEP_BEHIND = OptionGeneration.TRAIL_KEEP_BEHIND;
        GHOST_MODE_ENABLED = OptionGeneration.GHOST_MODE_ENABLED;
        GHOST_SHOW_TOP = OptionGeneration.GHOST_SHOW_TOP;
        GHOST_TRANSPARENCY = OptionGeneration.GHOST_TRANSPARENCY;
        JUMP_VALIDATION_ENABLED = OptionGeneration.JUMP_VALIDATION_ENABLED;
        MAX_JUMP_DISTANCE = OptionGeneration.MAX_JUMP_DISTANCE;
        MAX_HORIZONTAL_DISTANCE = OptionGeneration.MAX_HORIZONTAL_DISTANCE;
        MAX_VERTICAL_UP = OptionGeneration.MAX_VERTICAL_UP;
        MAX_VERTICAL_DOWN = OptionGeneration.MAX_VERTICAL_DOWN;
        JUMP_TYPES_ENABLED = OptionGeneration.JUMP_TYPES_ENABLED;
        JUMP_TYPE_ENABLED = OptionGeneration.JUMP_TYPE_ENABLED;
        JUMP_TYPE_CHANCE = OptionGeneration.JUMP_TYPE_CHANCE;

        // General
        BORDER_SIZE = OptionGeneral.BORDER_SIZE;
        POSSIBLE_LEADS = OptionGeneral.POSSIBLE_LEADS;
        HEADING = OptionGeneral.HEADING;
        GO_BACK_LOC = OptionGeneral.GO_BACK_LOC;
        OPTIONS_ENABLED = OptionGeneral.OPTIONS_ENABLED;
        OPTIONS_DEFAULTS = OptionGeneral.OPTIONS_DEFAULTS;
    }

    /** @deprecated Use {@link OptionStyles#initStyles(String, FileConfiguration, BiFunction)} */
    @Deprecated
    public static Set<Style> initStyles(String path, FileConfiguration config,
                                        BiFunction<String, List<Material>, Style> fn) {
        return OptionStyles.initStyles(path, config, fn);
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public enum ParticleShape { DOT, CIRCLE, BOX }
}
