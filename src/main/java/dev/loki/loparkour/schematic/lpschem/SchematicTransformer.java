package dev.loki.loparkour.schematic.lpschem;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Rotates a {@link LPSchematic} by 90/180/270 degrees around the Y axis.
 * Preserves block facing where possible via {@link Directional} BlockData.
 */
public class SchematicTransformer {

    private final LPSchematic source;

    public SchematicTransformer(@NotNull LPSchematic source) {
        this.source = source;
    }

    /**
     * Returns a new rotated schematic. The original is not modified.
     *
     * @param degrees 90, 180, or 270 (clockwise from above).
     */
    @NotNull
    public LPSchematic rotate(int degrees) {
        degrees = ((degrees % 360) + 360) % 360;
        if (degrees == 0) return source;

        SchematicDimensions dim = source.getDimensions();
        int origW = dim.width;
        int origH = dim.height;
        int origL = dim.length;

        // New dimensions after rotation
        int newW = (degrees == 180) ? origW : origL;
        int newL = (degrees == 180) ? origL : origW;

        int[] origBlocks = getBlocks();
        List<String> palette = getPalette();

        int[] rotated = new int[origW * origH * origL];

        for (int y = 0; y < origH; y++) {
            for (int z = 0; z < origL; z++) {
                for (int x = 0; x < origW; x++) {
                    int origIdx = x + (z * origW) + (y * origW * origL);
                    int paletteIdx = origBlocks[origIdx];

                    int nx, nz;
                    switch (degrees) {
                        case 90 -> { nx = origL - 1 - z; nz = x; }
                        case 180 -> { nx = origW - 1 - x; nz = origL - 1 - z; }
                        case 270 -> { nx = z; nz = origW - 1 - x; }
                        default -> { nx = x; nz = z; }
                    }

                    int newIdx = nx + (nz * newW) + (y * newW * newL);
                    rotated[newIdx] = paletteIdx;
                }
            }
        }

        // Rotate palette entries (block facing)
        List<String> rotatedPalette = rotatePalette(palette, degrees);

        SchematicDimensions newDim = new SchematicDimensions(newW, origH, newL);

        // Rotate markers
        SchematicMarkers origMarkers = source.getMarkers();
        SchematicMarkers newMarkers = rotateMarkers(origMarkers, origW, origL, newW, newL, degrees);

        return new LPSchematic(source.getMetadata(), newDim, rotatedPalette, rotated, newMarkers);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private int[] getBlocks() {
        return source.getBlockArray();
    }

    private List<String> getPalette() {
        return new ArrayList<>(source.getPalette());
    }

    private List<String> rotatePalette(List<String> palette, int degrees) {
        List<String> result = new ArrayList<>(palette.size());
        for (String blockStr : palette) {
            result.add(rotateBlockData(blockStr, degrees));
        }
        return result;
    }

    private String rotateBlockData(String blockDataStr, int degrees) {
        try {
            BlockData bd = Bukkit.createBlockData(blockDataStr);
            if (bd instanceof Directional dir) {
                for (int i = 0; i < degrees / 90; i++) {
                    dir.setFacing(rotateDirection(dir.getFacing()));
                }
                return dir.getAsString();
            }
            return blockDataStr;
        } catch (Exception e) {
            return blockDataStr;
        }
    }

    private org.bukkit.block.BlockFace rotateDirection(org.bukkit.block.BlockFace face) {
        return switch (face) {
            case NORTH -> org.bukkit.block.BlockFace.EAST;
            case EAST  -> org.bukkit.block.BlockFace.SOUTH;
            case SOUTH -> org.bukkit.block.BlockFace.WEST;
            case WEST  -> org.bukkit.block.BlockFace.NORTH;
            default    -> face;
        };
    }

    private SchematicMarkers rotateMarkers(SchematicMarkers m,
                                           int origW, int origL,
                                           int newW, int newL,
                                           int degrees) {
        var start = rotateVec(m.getStart(), origW, origL, degrees);
        var end   = rotateVec(m.getEnd(),   origW, origL, degrees);

        SchematicMarkers result = new SchematicMarkers(start, end);
        for (var cp : m.getCheckpoints()) {
            result.addCheckpoint(rotateVec(cp, origW, origL, degrees));
        }
        return result;
    }

    private SchematicMarkers.Vector3i rotateVec(SchematicMarkers.Vector3i v,
                                                int origW, int origL, int degrees) {
        return switch (degrees) {
            case 90  -> new SchematicMarkers.Vector3i(origL - 1 - v.z, v.y, v.x);
            case 180 -> new SchematicMarkers.Vector3i(origW - 1 - v.x, v.y, origL - 1 - v.z);
            case 270 -> new SchematicMarkers.Vector3i(v.z, v.y, origW - 1 - v.x);
            default  -> new SchematicMarkers.Vector3i(v.x, v.y, v.z);
        };
    }
}
