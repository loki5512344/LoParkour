package dev.loki.loparkour.schematic.schem;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SchemPaster {

    private SchemPaster() {
    }

    @NotNull
    public static List<Block> paste(@NotNull Clipboard clipboard, @NotNull Location origin, @NotNull World world) {
        BlockVector3 pasteAt = BlockVector3.at(
                origin.getBlockX(),
                origin.getBlockY(),
                origin.getBlockZ()
        );
        BlockVector3 min = clipboard.getMinimumPoint();

        List<Block> placed = new ArrayList<>();
        for (BlockVector3 pos : clipboard.getRegion()) {
            BlockState weBlock = clipboard.getBlock(pos);
            if (weBlock.getBlockType().getMaterial().isAir()) {
                continue;
            }

            BlockVector3 dest = pasteAt.add(pos.subtract(min));
            Block block = world.getBlockAt(dest.x(), dest.y(), dest.z());
            try {
                block.setBlockData(Bukkit.createBlockData(weBlock.getAsString()), false);
                placed.add(block);
            } catch (IllegalArgumentException ignored) {
                // Block state not valid on this server version
            }
        }
        return placed;
    }
}
