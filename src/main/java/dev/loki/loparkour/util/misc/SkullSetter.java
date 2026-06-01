package dev.loki.loparkour.util.misc;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Utility for setting player heads
 */
public class SkullSetter {
    
    public static void setPlayerHead(OfflinePlayer player, SkullMeta meta) {
        if (meta != null && player != null) {
            meta.setOwningPlayer(player);
        }
    }
}
