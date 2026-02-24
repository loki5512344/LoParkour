package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourSpectator;
import dev.efnilite.vilib.event.EventWrapper;

/**
 * Gets called when a player starts spectating a session. Read-only.
 *
 * @author loki
 * @since 5.0.0
 */
public class ParkourSpectateEvent extends EventWrapper {

    public final ParkourSpectator player;

    public ParkourSpectateEvent(ParkourSpectator player) {
        this.player = player;
    }
}
