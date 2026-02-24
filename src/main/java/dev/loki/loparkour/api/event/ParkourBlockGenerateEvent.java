package dev.loki.loparkour.api.event;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.efnilite.vilib.event.EventWrapper;
import org.bukkit.block.Block;

import java.util.List;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author loki
 * @since 5.0.0
 */
public class ParkourBlockGenerateEvent extends EventWrapper {

    public final List<Block> blocks;
    public final ParkourGenerator generator;
    public final ParkourPlayer player;

    public ParkourBlockGenerateEvent(List<Block> blocks, ParkourGenerator generator, ParkourPlayer player) {
        this.blocks = blocks;
        this.generator = generator;
        this.player = player;
    }
}
