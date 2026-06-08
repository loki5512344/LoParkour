package dev.loki.loparkour.util.misc;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for Material handling.
 */
public final class MaterialUtil {

    private MaterialUtil() {
        // Utility class, no instantiation
    }

    /**
     * Check if material is a slab type.
     */
    public static boolean isSlabMaterial(@NotNull Material material) {
        return material == Material.SMOOTH_QUARTZ_SLAB ||
               material == Material.STONE_SLAB ||
               material.name().endsWith("_SLAB");
    }
}
