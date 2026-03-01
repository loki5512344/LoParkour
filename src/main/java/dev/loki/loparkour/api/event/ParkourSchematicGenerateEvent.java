package dev.loki.loparkour.api.event;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author loki
 * @since 5.0.0
 */
public class ParkourSchematicGenerateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public final dev.loki.loparkour.schematic.lpschem.LPSchematic schematic;
    public final ParkourGenerator generator;
    public final ParkourPlayer player;

    public ParkourSchematicGenerateEvent(dev.loki.loparkour.schematic.lpschem.LPSchematic schematic, ParkourGenerator generator, ParkourPlayer player) {
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
