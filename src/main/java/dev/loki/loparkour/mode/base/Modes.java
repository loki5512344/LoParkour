package dev.loki.loparkour.mode.base;

import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.mode.impl.*;

public class Modes {

    public static DefaultMode DEFAULT;
    public static SpectatorMode SPECTATOR;
    public static SpeedrunMode SPEEDRUN;
    public static ElytraMode ELYTRA;
    public static RaceMode RACE;
    public static CoopMode COOP;
    public static GravityShiftMode GRAVITY_SHIFT;
    public static HardcoreMode HARDCORE;

    public static void init() {
        DEFAULT = (DefaultMode) Registry.getMode("default");
        SPECTATOR = (SpectatorMode) Registry.getMode("spectator");
        SPEEDRUN = (SpeedrunMode) Registry.getMode("speedrun");
        ELYTRA = (ElytraMode) Registry.getMode("elytra");
        RACE = (RaceMode) Registry.getMode("race");
        COOP = (CoopMode) Registry.getMode("coop");
        GRAVITY_SHIFT = (GravityShiftMode) Registry.getMode("gravity-shift");
        HARDCORE = (HardcoreMode) Registry.getMode("hardcore");
    }
}
