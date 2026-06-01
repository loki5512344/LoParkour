package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.core.ParkourUser;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Gets called when a player leaves a session. Read-only.
 */
public class ParkourLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourUser player;

    public ParkourLeaveEvent(ParkourUser player) {
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
