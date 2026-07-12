package dev.loki.loparkour.mode.base;

import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.mode.impl.CoopMode;
import dev.loki.loparkour.mode.impl.DefaultMode;
import dev.loki.loparkour.mode.impl.GravityShiftMode;
import dev.loki.loparkour.mode.impl.InvisibleBarrierMode;
import dev.loki.loparkour.mode.impl.RaceMode;
import dev.loki.loparkour.mode.impl.SpectatorMode;
import dev.loki.loparkour.mode.impl.SpeedrunMode;

public final class Modes {

    private Modes() {
    }

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
