package dev.loki.loparkour.schematic.nbt;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StructurePaster {

    private StructurePaster() {
    }

    @NotNull
    public static List<Block> paste(@NotNull Structure structure, @NotNull Location origin, @NotNull World world) {
        structure.place(
                origin,
                false,
                StructureRotation.NONE,
                Mirror.NONE,
                -1,
                1.0f,
                new Random()
        );

        BlockVector size = structure.getSize();
        int width = size.getBlockX();
        int height = size.getBlockY();
        int length = size.getBlockZ();

        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();

        List<Block> placed = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                    BlockData data = block.getBlockData();
                    if (!data.getMaterial().isAir()) {
                        placed.add(block);
                    }
                }
            }
        }
        return placed;
    }
}
