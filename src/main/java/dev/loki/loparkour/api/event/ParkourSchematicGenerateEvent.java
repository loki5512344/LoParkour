package dev.loki.loparkour.api.event;

import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.schematic.core.ParkourSchematic;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Gets called when a schematic jump is pasted. Read-only.
 */
public class ParkourSchematicGenerateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourSchematic schematic;
    public final ParkourGenerator generator;
    public final ParkourPlayer player;

    public ParkourSchematicGenerateEvent(ParkourSchematic schematic, ParkourGenerator generator, ParkourPlayer player) {
        this.schematic = schematic;
        this.generator = generator;
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
