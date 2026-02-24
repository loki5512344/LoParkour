package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a point is scored. Read-only.
 */
public class ParkourScoreEvent extends EventWrapper {

    public final ParkourPlayer player;

    public ParkourScoreEvent(ParkourPlayer player) {
        this.player = player;
    }
}
