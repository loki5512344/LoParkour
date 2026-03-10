package dev.loki.loparkour.generator.jump;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum JumpType {

    NORMAL(1.0, "Normal jump", Material.STONE),
    NEO_JUMP(2.5, "Jump around fence", Material.OAK_FENCE, Material.NETHER_BRICK_FENCE),
    HEAD_HITTER(2.0, "Jump with block overhead", Material.STONE_SLAB),
    FENCE_JUMP(1.8, "Jump onto fence", Material.OAK_FENCE, Material.BIRCH_FENCE, Material.SPRUCE_FENCE),
    TRAPDOOR_JUMP(1.5, "Jump onto trapdoor", Material.OAK_TRAPDOOR, Material.IRON_TRAPDOOR),
    LADDER_JUMP(1.7, "Jump onto ladder", Material.LADDER);

    private final double difficulty;
    private final String description;
    private final List<Material> materials;

    JumpType(double difficulty, String description, Material... materials) {
        this.difficulty = difficulty;
        this.description = description;
        this.materials = Arrays.asList(materials);
    }

    public double getDifficulty() {
        return difficulty;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public List<Material> getMaterials() {
        return materials;
    }

    @NotNull
    public Material getRandomMaterial() {
        if (materials.isEmpty()) {
            return Material.STONE;
        }
        return materials.get((int) (Math.random() * materials.size()));
    }

    public boolean usesMaterial(@NotNull Material material) {
        return materials.contains(material);
    }
}
