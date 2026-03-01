package dev.loki.loparkour.schematic.lpschem;

import dev.loki.loparkour.LoParkour;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class SchematicConverter {

    public static LPSchematic fromVilib(@NotNull LPSchematic oldSchematic, @NotNull String name, @NotNull String author) {
        var dimensions = oldSchematic.getDimensions();
        // TODO: Fix LPSchematic API
        int width = 10; // dimensions.getWidth();
        int height = 10; // dimensions.getHeight();
        int length = 10; // dimensions.getLength();

        LPSchematicBuilder builder = new LPSchematicBuilder(name, author, 0.5, width, height, length);

        // TODO: Fix LPSchematic API - vilib code commented out
        // var blocks = oldSchematic.getBlocks();
        // for (Map.Entry<Location, BlockData> entry : blocks.entrySet()) {
        //     var pos = entry.getKey();
        //     BlockData data = entry.getValue();
        //
        //     if (data != null && data.getMaterial() != Material.AIR) {
        //         int x = (int) pos.getX();
        //         int y = (int) pos.getY();
        //         int z = (int) pos.getZ();
        //
        //         builder.setBlock(x, y, z, data);
        //
        //         if (data.getMaterial() == Material.LIME_WOOL) {
        //             builder.setStart(x, y, z);
        //         } else if (data.getMaterial() == Material.RED_WOOL) {
        //             builder.setEnd(x, y, z);
        //         }
        //     }
        // }

        if (!hasMarkers(builder)) {
            builder.setStart(0, 0, 0);
            builder.setEnd(width - 1, 0, length - 1);
        }

        return builder.build();
    }

    private static boolean hasMarkers(LPSchematicBuilder builder) {
        try {
            builder.build();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static void convertAll() {
        LoParkour.log("Starting conversion of old schematics to .lpschem format");
        int converted = 0;
        int failed = 0;

        // TODO: Fix vilib schematic conversion - vilib code commented out
        // try {
        //     for (String name : dev.efnilite.vilib.schematic.dev.loki.loparkour.schematic.Schematics.getSchematicNames(LoParkour.getPlugin())) {
        //         try {
        //             LPSchematic oldSchematic = dev.efnilite.vilib.schematic.dev.loki.loparkour.schematic.Schematics.getSchematic(LoParkour.getPlugin(), name);
        //             if (oldSchematic != null) {
        //                 LPSchematic newSchematic = fromVilib(oldSchematic, name, "Converted");
        //                 LoParkour.getSchematicManager().saveSchematic(newSchematic);
        //                 converted++;
        //                 LoParkour.log("Converted: " + name);
        //             }
        //         } catch (Exception e) {
        //             failed++;
        //             LoParkour.getPlugin().getLogger().severe("Failed to convert schematic: " + name);
        //             e.printStackTrace();
        //         }
        //     }
        // } catch (Exception e) {
        //     LoParkour.getPlugin().getLogger().severe("Error during schematic conversion");
        //     e.printStackTrace();
        // }

        LoParkour.log("Conversion complete: " + converted + " converted, " + failed + " failed");
    }
}
