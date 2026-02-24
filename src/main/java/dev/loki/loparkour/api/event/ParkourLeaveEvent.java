package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourUser;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player leaves a session. Read-only.
 */
public class ParkourLeaveEvent extends EventWrapper {

    public final ParkourUser player;

    public ParkourLeaveEvent(ParkourUser player) {
        this.player = player;
    }
}
