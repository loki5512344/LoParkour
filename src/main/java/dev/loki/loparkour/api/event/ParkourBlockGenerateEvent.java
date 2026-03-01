package dev.loki.loparkour.api.event;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.block.Block;

import java.util.List;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author loki
 * @since 5.0.0
 */
public class ParkourBlockGenerateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public final List<Block> blocks;
    public final ParkourGenerator generator;
    public final ParkourPlayer player;

    public ParkourBlockGenerateEvent(List<Block> blocks, ParkourGenerator generator, ParkourPlayer player) {
        this.blocks = blocks;
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
