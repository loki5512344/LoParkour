package dev.loki.loparkour.generator.jump.placement;

import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.generator.jump.calculation.JumpType;
import dev.loki.loparkour.style.core.Style;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles selection of block types and materials for parkour generation.
 */
public class BlockSelector {
    
    private final ParkourGenerator generator;
    
    public BlockSelector(@NotNull ParkourGenerator generator) {
        this.generator = generator;
    }
    
    private static final int MAX_STYLE_PICKS = 64;

    /**
     * Select block data for the next block based on current style.
     */
    @NotNull
    public BlockData selectBlockData() {
        Style style = Registry.getStyle(generator.profile.get("style").value());
        if (style == null) {
            generator.profile.set("style", Registry.getStyles().stream().findFirst().orElseThrow().getName());
            return selectBlockData();
        }
        for (int i = 0; i < MAX_STYLE_PICKS; i++) {
            Material mat = style.getNext();
            if (!isUnsafeParkourMaterial(mat)) {
                return mat.createBlockData();
            }
        }
        return Material.STONE.createBlockData();
    }

    /**
     * Blocks whose vanilla {@code onPlace} scans neighbors (e.g. iron golem pattern for carved pumpkin)
     * can NPE on high/void parkour worlds when adjacent chunks have no loaded state.
     */
    static boolean isUnsafeParkourMaterial(@NotNull Material m) {
        if (m == Material.CARVED_PUMPKIN || m == Material.JACK_O_LANTERN || m == Material.PUMPKIN) {
            return true;
        }
        if (m == Material.DRAGON_EGG) {
            return true;
        }
        String n = m.name();
        if (n.endsWith("_BED")) {
            return true;
        }
        return false;
    }
    
    /**
     * Select jump type based on current configuration and probabilities.
     */
    @NotNull
    public JumpType selectJumpType() {
        // Simple random selection for now
        JumpType[] types = JumpType.values();
        return types[(int) (Math.random() * types.length)];
    }
    
    /**
     * Check if material is a slab type.
     */
    public boolean isSlabMaterial(@NotNull Material material) {
        return material == Material.SMOOTH_QUARTZ_SLAB ||
               material == Material.STONE_SLAB ||
               material.name().endsWith("_SLAB");
    }
    
    /**
     * Get a slab material from the current player's style.
     */
    @Nullable
    public Material getSlabMaterialFromCurrentStyle() {
        try {
            Style style = Registry.getStyle(generator.profile.get("style").value());
            if (style == null) return null;
            
            // Try to get a slab material from the style multiple times
            for (int i = 0; i < 20; i++) {
                Material material = style.getNext();
                if (isSlabMaterial(material)) {
                    return material;
                }
            }
        } catch (Exception e) {
            // Ignore errors and return null
        }
        return null;
    }
    
    /**
     * Check if material is a special block type that affects jump mechanics.
     */
    public boolean isSpecialMaterial(@NotNull Material material) {
        return isSlabMaterial(material) ||
               material == Material.GLASS_PANE ||
               material == Material.PACKED_ICE ||
               material == Material.BLUE_ICE ||
               material == Material.ICE ||
               isFenceMaterial(material) ||
               isTrapdoorMaterial(material) ||
               material == Material.LADDER;
    }
    
    public boolean isFenceMaterial(@NotNull Material material) {
        return material == Material.OAK_FENCE ||
               material == Material.BIRCH_FENCE ||
               material == Material.SPRUCE_FENCE ||
               material == Material.DARK_OAK_FENCE ||
               material == Material.JUNGLE_FENCE ||
               material == Material.ACACIA_FENCE ||
               material == Material.MANGROVE_FENCE ||
               material == Material.CHERRY_FENCE ||
               material == Material.CRIMSON_FENCE ||
               material == Material.WARPED_FENCE ||
               material == Material.NETHER_BRICK_FENCE;
    }
    
    public boolean isTrapdoorMaterial(@NotNull Material material) {
        return material == Material.OAK_TRAPDOOR ||
               material == Material.BIRCH_TRAPDOOR ||
               material == Material.SPRUCE_TRAPDOOR ||
               material == Material.DARK_OAK_TRAPDOOR ||
               material == Material.JUNGLE_TRAPDOOR ||
               material == Material.ACACIA_TRAPDOOR ||
               material == Material.MANGROVE_TRAPDOOR ||
               material == Material.CHERRY_TRAPDOOR ||
               material == Material.CRIMSON_TRAPDOOR ||
               material == Material.WARPED_TRAPDOOR ||
               material == Material.IRON_TRAPDOOR;
    }
}
