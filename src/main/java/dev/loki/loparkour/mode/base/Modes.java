package dev.loki.loparkour.mode.base;

import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.mode.impl.*;

public class Modes {

    public static DefaultMode DEFAULT;
    public static SpectatorMode SPECTATOR;
    public static SpeedrunMode SPEEDRUN;
    public static InvisibleBarrierMode INVISIBLE_BARRIER;
    public static RaceMode RACE;
    public static CoopMode COOP;
    public static GravityShiftMode GRAVITY_SHIFT;

    public static void init() {
        DEFAULT = (DefaultMode) Registry.getMode("default");
        SPECTATOR = (SpectatorMode) Registry.getMode("spectator");
        SPEEDRUN = (SpeedrunMode) Registry.getMode("speedrun");
        INVISIBLE_BARRIER = (InvisibleBarrierMode) Registry.getMode("invisible-barrier");
        RACE = (RaceMode) Registry.getMode("race");
        COOP = (CoopMode) Registry.getMode("coop");
        GRAVITY_SHIFT = (GravityShiftMode) Registry.getMode("gravity-shift");
    }
}
