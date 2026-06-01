package dev.loki.loparkour.schematic.schem;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import dev.loki.loparkour.schematic.legacy.lpschem.LPSchematicLegacy;
import dev.loki.loparkour.schematic.legacy.lpschem.SchematicDimensions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SchematicClipboardBuilder {

    private SchematicClipboardBuilder() {
    }

    @NotNull
    public static Clipboard fromSelection(@NotNull Location pos1, @NotNull Location pos2) {
        if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
            throw new IllegalArgumentException("Selection must be in one world");
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        BlockVector3 min = BlockVector3.at(minX, minY, minZ);
        BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
        CuboidRegion region = new CuboidRegion(min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        World world = pos1.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isAir()) {
                        continue;
                    }
                    BlockVector3 rel = BlockVector3.at(x, y, z).subtract(min);
                    try {
                        BlockData data = block.getBlockData();
                        BlockState state = BukkitAdapter.adapt(data);
                        clipboard.setBlock(rel, state);
                    } catch (IllegalArgumentException ignored) {
                        // Unknown block on this version
                    }
                }
            }
        }
        return clipboard;
    }

    @NotNull
    public static Clipboard fromLegacy(@NotNull LPSchematicLegacy legacy) {
        SchematicDimensions dim = legacy.getDimensions();
        BlockVector3 min = BlockVector3.at(0, 0, 0);
        BlockVector3 max = BlockVector3.at(dim.width - 1, dim.height - 1, dim.length - 1);
        CuboidRegion region = new CuboidRegion(min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        List<String> palette = legacy.getPalette();
        int[] blocks = legacy.getBlockArray();
        int width = dim.width;
        int length = dim.length;

        for (int y = 0; y < dim.height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = x + (z * width) + (y * width * length);
                    int paletteIndex = blocks[index];
                    if (paletteIndex <= 0 || paletteIndex >= palette.size()) {
                        continue;
                    }
                    String blockData = palette.get(paletteIndex);
                    if (blockData == null || blockData.isBlank()) {
                        continue;
                    }
                    try {
                        BlockData data = org.bukkit.Bukkit.createBlockData(blockData);
                        BlockState state = BukkitAdapter.adapt(data);
                        if (state.getBlockType().getMaterial().isAir()) {
                            continue;
                        }
                        clipboard.setBlock(BlockVector3.at(x, y, z), state);
                    } catch (IllegalArgumentException ignored) {
                        // Skip invalid states
                    }
                }
            }
        }
        return clipboard;
    }
}
