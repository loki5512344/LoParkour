package dev.loki.loparkour.api.event;

import dev.loki.loparkour.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Gets called when a player falls. Read-only.
 */
public class ParkourFallEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourPlayer player;

    public ParkourFallEvent(ParkourPlayer player) {
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
