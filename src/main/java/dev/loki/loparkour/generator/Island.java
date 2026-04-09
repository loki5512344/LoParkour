package dev.loki.loparkour.generator;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.util.Materials;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawn area: flat platform at the session cell (no island schematic).
 */
public final class Island {

    /** Half-size: platform is (2*R+1) x (2*R+1) blocks. */
    private static final int PLATFORM_RADIUS = 4;

    public final Session session;
    /**
     * Unused for spawn; kept for API / {@link dev.loki.loparkour.generator.jump.BlockPlacer} schematic branch.
     */
    public final dev.loki.loparkour.schematic.lpschem.LPSchematic schematic;

    public List<Block> blocks;

    public Island(@NotNull Session session, @Nullable dev.loki.loparkour.schematic.lpschem.LPSchematic schematic) {
        this.session = session;
        this.schematic = schematic;
    }

    public void build(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        int cx = location.getBlockX();
        int cy = location.getBlockY();
        int cz = location.getBlockZ();

        BlockFace face = Option.HEADING;
        int hx = face.getModX();
        int hz = face.getModZ();

        if (session.generator.state.heading == null) {
            session.generator.state.heading = face.getDirection();
        }
        Vector h = session.generator.state.heading;

        String firstBlockStr = Config.GENERATION.getString("advanced.island.parkour.first-block-material");
        if (firstBlockStr.isBlank()) {
            firstBlockStr = "stone";
        }
        Material platformMat = Materials.parseOr(firstBlockStr, Material.STONE);

        List<Block> placed = new ArrayList<>();
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                Block b = world.getBlockAt(cx + dx, cy, cz + dz);
                b.setType(platformMat);
                placed.add(b);
            }
        }

        // Player spawns at center, first parkour block 6 blocks forward and 1 block up
        int px = cx;
        int pz = cz;
        int qx = cx + 6 * hx;
        int qz = cz + 6 * hz;
        int qy = cy + 1;

        Block playerFoot = world.getBlockAt(px, cy, pz);

        Location ps = playerFoot.getLocation().add(0.5, 1.0, 0.5);
        ps.setYaw((float) Config.GENERATION.getInt("advanced.island.spawn.yaw"));
        ps.setPitch((float) Config.GENERATION.getInt("advanced.island.spawn.pitch"));

        Location parkourStart = world.getBlockAt(qx, qy, qz).getLocation();

        session.generator.generateFirst(ps, parkourStart);
        session.generator.startTick();
        session.getPlayers().forEach(pp -> pp.setup(ps));

        this.blocks = placed;
    }

    public void destroy() {
        if (blocks == null) {
            return;
        }

        blocks.forEach(block -> block.setType(Material.AIR, false));
    }
}
