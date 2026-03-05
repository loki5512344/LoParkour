package dev.loki.loparkour.generator;

import java.util.ArrayList;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.session.Session;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Spawn island handler.
 *
 * @author loki
 * @since 5.0.0
 */
public final class Island {

    /**
     * The session.
     */
    public final Session session;
    /**
     * The schematic.
     */
    public final dev.loki.loparkour.schematic.lpschem.LPSchematic schematic;

    /**
     * The blocks that have been affected by the schematic.
     */
    public List<Block> blocks;

    public Island(@NotNull Session session, @Nullable dev.loki.loparkour.schematic.lpschem.LPSchematic schematic) {
        this.session = session;
        this.schematic = schematic;
    }

    /**
     * Builds the island and teleports the player.
     */
    public void build(Location location) {
        if (schematic == null) {
            LoParkour.getPlugin().getLogger().warning("Cannot build island - schematic is null!");
            return;
        }

        blocks = schematic.paste(location, location.getWorld());

        Material playerMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.spawn.player-block").toUpperCase());
        Material parkourMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.parkour.begin-block").toUpperCase());

        try {
            Block player = blocks.stream().filter(block -> block.getType() == playerMaterial).findAny().orElseThrow();
            Block parkour = blocks.stream().filter(block -> block.getType() == parkourMaterial).findAny().orElseThrow();

            player.setType(Material.STONE);
            parkour.setType(Material.STONE);

            Location ps = player.getLocation().add(0.5, 1.0, 0.5);
            ps.setYaw(Config.GENERATION.getInt("advanced.island.spawn.yaw"));
            ps.setPitch(Config.GENERATION.getInt("advanced.island.spawn.pitch"));

            // First parkour block should be generated FROM the parkour start block
            // in the direction of heading, not AT the parkour start block
            Location parkourStart = parkour.getLocation().add(session.generator.heading);
            
            session.generator.generateFirst(ps, parkourStart);
            session.generator.startTick();
            session.getPlayers().forEach(pp -> pp.setup(ps));
        } catch (NoSuchElementException ex) {
            LoParkour.getPlugin().getLogger().severe("Error while trying to find parkour or player spawn in schematic %s - check if you used the same material as the one in generation.yml - ".formatted("schematic") + ex.getMessage());

            blocks.forEach(block -> block.setType(Material.AIR));
        }
    }

    /**
     * Destroys the island.
     */
    public void destroy() {
        if (blocks == null) {
            return;
        }

        blocks.forEach(block -> block.setType(Material.AIR, false));
    }
}
