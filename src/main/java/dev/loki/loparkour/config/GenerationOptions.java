package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;

/**
 * Generation and jump-validation settings. Populated by {@link Option#init}.
 */
public final class GenerationOptions {

    // Block type chances
    public static double TYPE_NORMAL;
    public static double TYPE_SPECIAL;
    public static double TYPE_SCHEMATICS;

    // Special block chances
    public static double SPECIAL_ICE;
    public static double SPECIAL_SLAB;
    public static double SPECIAL_PANE;
    public static double SPECIAL_FENCE;

    // Distance weights
    public static double NORMAL_DISTANCE_1;
    public static double NORMAL_DISTANCE_2;
    public static double NORMAL_DISTANCE_3;
    public static double NORMAL_DISTANCE_4;

    // Height weights
    public static double NORMAL_HEIGHT_1;
    public static double NORMAL_HEIGHT_0;
    public static double NORMAL_HEIGHT_NEG1;
    public static double NORMAL_HEIGHT_NEG2;

    // World bounds
    public static int MAX_Y;
    public static int MIN_Y;

    // Jump validation
    public static boolean JUMP_VALIDATION_ENABLED;
    public static double  MAX_JUMP_DISTANCE;
    public static double  MAX_HORIZONTAL_DISTANCE;
    public static double  MAX_VERTICAL_UP;
    public static double  MAX_VERTICAL_DOWN;

    // Jump types
    public static boolean                    JUMP_TYPES_ENABLED;
    public static java.util.Map<String, Boolean> JUMP_TYPE_ENABLED;
    public static java.util.Map<String, Double>  JUMP_TYPE_CHANCE;

    // Memory optimisation
    public static int BLOCK_CLEANUP_DISTANCE;
    public static int CLEANUP_INTERVAL;

    // Ghost mode
    public static boolean GHOST_MODE_ENABLED;
    public static int     GHOST_SHOW_TOP;
    public static double  GHOST_TRANSPARENCY;

    static void init() {
        // Jump validation
        JUMP_VALIDATION_ENABLED  = Config.CONFIG.isPath("jump-validation.enabled") && Config.CONFIG.getBoolean("jump-validation.enabled");
        MAX_JUMP_DISTANCE        = Config.CONFIG.isPath("jump-validation.max-distance")    ? Config.CONFIG.getDouble("jump-validation.max-distance")    : 5.0;
        MAX_HORIZONTAL_DISTANCE  = Config.CONFIG.isPath("jump-validation.max-horizontal")  ? Config.CONFIG.getDouble("jump-validation.max-horizontal")  : 4.0;
        MAX_VERTICAL_UP          = Config.CONFIG.isPath("jump-validation.max-vertical-up") ? Config.CONFIG.getDouble("jump-validation.max-vertical-up") : 1.0;
        MAX_VERTICAL_DOWN        = Config.CONFIG.isPath("jump-validation.max-vertical-down") ? Config.CONFIG.getDouble("jump-validation.max-vertical-down") : 3.0;

        // Jump types
        JUMP_TYPES_ENABLED = Config.CONFIG.isPath("jump-types.enabled") && Config.CONFIG.getBoolean("jump-types.enabled");
        JUMP_TYPE_ENABLED  = new java.util.HashMap<>();
        JUMP_TYPE_CHANCE   = new java.util.HashMap<>();
        if (Config.CONFIG.isPath("jump-types.types")) {
            for (String type : Config.CONFIG.getChildren("jump-types.types")) {
                String path = "jump-types.types." + type;
                JUMP_TYPE_ENABLED.put(type, Config.CONFIG.isPath(path + ".enabled") && Config.CONFIG.getBoolean(path + ".enabled"));
                JUMP_TYPE_CHANCE.put(type,  Config.CONFIG.isPath(path + ".chance")  ? Config.CONFIG.getDouble(path + ".chance") : 0.0);
            }
        }

        // Memory
        BLOCK_CLEANUP_DISTANCE = Config.CONFIG.isPath("memory.block-cleanup-distance") ? Config.CONFIG.getInt("memory.block-cleanup-distance") : 100;
        CLEANUP_INTERVAL       = Config.CONFIG.isPath("memory.cleanup-interval")       ? Config.CONFIG.getInt("memory.cleanup-interval")       : 100;

        // Ghost
        GHOST_MODE_ENABLED  = Config.CONFIG.isPath("ghost-mode.enabled")      && Config.CONFIG.getBoolean("ghost-mode.enabled");
        GHOST_SHOW_TOP      = Config.CONFIG.isPath("ghost-mode.show-top")      ? Config.CONFIG.getInt("ghost-mode.show-top")      : 3;
        GHOST_TRANSPARENCY  = Config.CONFIG.isPath("ghost-mode.transparency")  ? Config.CONFIG.getDouble("ghost-mode.transparency") : 0.5;

        // Generation chances
        TYPE_NORMAL      = Config.GENERATION.getInt("generation.type.normal")    / 100.0;
        TYPE_SPECIAL     = Config.GENERATION.getInt("generation.type.special")   / 100.0;
        TYPE_SCHEMATICS  = Config.GENERATION.getInt("generation.type.schematic") / 100.0;

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

    private GenerationOptions() {}
}
