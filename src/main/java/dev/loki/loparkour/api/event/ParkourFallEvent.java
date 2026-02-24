package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player falls. Read-only.
 */
public class ParkourFallEvent extends EventWrapper {

    public final ParkourPlayer player;

    public ParkourFallEvent(ParkourPlayer player) {
        this.player = player;
    }
}
