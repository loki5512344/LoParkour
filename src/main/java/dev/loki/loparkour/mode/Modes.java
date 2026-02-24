package dev.loki.loparkour.mode;

import dev.loki.loparkour.api.Registry;

public class Modes {

    public static DefaultMode DEFAULT;
    public static SpectatorMode SPECTATOR;

    public static void init() {
        DEFAULT = (DefaultMode) Registry.getMode("default");
        SPECTATOR = (SpectatorMode) Registry.getMode("spectator");
    }
}
