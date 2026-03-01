package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourUser;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Gets called when a player joins a session. Read-only.
 */
public class ParkourJoinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourUser player;

    public ParkourJoinEvent(ParkourUser player) {
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
