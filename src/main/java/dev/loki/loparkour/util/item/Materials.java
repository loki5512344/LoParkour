package dev.loki.loparkour.util.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Parses {@link Material} names from config (modern IDs, registry, legacy aliases).
 * On older servers (e.g. 1.21.1), colored {@code *_concrete_slab} / {@code *_terracotta_slab}
 * may be absent — those map to the matching full block.
 */
public final class Materials {

    private Materials() {}

    @Nullable
    public static Material parse(@Nullable String name) {
        if (name == null) {
            return null;
        }
        String raw = name.trim();
        if (raw.isEmpty()) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT).replace(' ', '_');

        Material m = Material.matchMaterial(key, false);
        if (m != null) {
            return m;
        }

        m = fromRegistry(key);
        if (m != null) {
            return m;
        }

        m = Material.matchMaterial(raw.toUpperCase(Locale.ROOT), false);
        if (m != null) {
            return m;
        }

        int colon = key.indexOf(':');
        if (colon >= 0) {
            String id = key.substring(colon + 1);
            m = Material.matchMaterial(id, false);
            if (m != null) {
                return m;
            }
            m = fromRegistry(id);
            if (m != null) {
                return m;
            }
        }

        m = coloredSlabFallback(key);
        if (m != null) {
            return m;
        }

        m = Material.matchMaterial(key, true);
        if (m != null) {
            return m;
        }

        return Material.getMaterial(raw.toUpperCase(Locale.ROOT));
    }

    @Nullable
    private static Material fromRegistry(String minecraftId) {
        try {
            NamespacedKey nk = NamespacedKey.minecraft(minecraftId);
            return Registry.MATERIAL.get(nk);
        } catch (IllegalArgumentException | NoSuchFieldError | NoClassDefFoundError ignored) {
            return null;
        }
    }

    /**
     * 1.21.2+ slab types are missing on 1.21.1 — use the solid concrete / terracotta block instead.
     */
    @Nullable
    private static Material coloredSlabFallback(String key) {
        if (key.endsWith("_concrete_slab")) {
            String blockId = key.substring(0, key.length() - "_concrete_slab".length()) + "_concrete";
            Material m = Material.matchMaterial(blockId, false);
            if (m != null) {
                return m;
            }
            return fromRegistry(blockId);
        }
        if (key.endsWith("_terracotta_slab")) {
            String blockId = key.substring(0, key.length() - "_terracotta_slab".length()) + "_terracotta";
            Material m = Material.matchMaterial(blockId, false);
            if (m != null) {
                return m;
            }
            return fromRegistry(blockId);
        }
        return null;
    }

    @NotNull
    public static Material parseOr(@Nullable String name, @NotNull Material fallback) {
        Material m = parse(name);
        return m != null ? m : fallback;
    }
}
