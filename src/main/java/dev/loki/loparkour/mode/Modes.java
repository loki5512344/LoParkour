package dev.loki.loparkour.mode;

import dev.loki.loparkour.api.Registry;

public class Modes {

    public static DefaultMode DEFAULT;
    public static SpectatorMode SPECTATOR;
    public static SpeedrunMode SPEEDRUN;
    public static GravityShiftMode GRAVITY_SHIFT;
    public static HardcoreMode HARDCORE;

    public static void init() {
        DEFAULT = (DefaultMode) Registry.getMode("default");
        SPECTATOR = (SpectatorMode) Registry.getMode("spectator");
        SPEEDRUN = (SpeedrunMode) Registry.getMode("speedrun");
        GRAVITY_SHIFT = (GravityShiftMode) Registry.getMode("gravity-shift");
        HARDCORE = (HardcoreMode) Registry.getMode("hardcore");
    }
}
