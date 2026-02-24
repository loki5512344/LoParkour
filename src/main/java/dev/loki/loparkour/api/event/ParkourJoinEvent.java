package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourUser;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player joins a session. Read-only.
 */
public class ParkourJoinEvent extends EventWrapper {

    public final ParkourUser player;

    public ParkourJoinEvent(ParkourUser player) {
        this.player = player;
    }
}
