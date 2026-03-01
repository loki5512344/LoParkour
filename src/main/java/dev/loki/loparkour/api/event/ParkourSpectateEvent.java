package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourSpectator;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Gets called when a player starts spectating a session. Read-only.
 *
 * @author loki
 * @since 5.0.0
 */
public class ParkourSpectateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourSpectator player;

    public ParkourSpectateEvent(ParkourSpectator player) {
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public void call() {
        Bukkit.getPluginManager().callEvent(this);
    }
}
