package dev.loki.loparkour.util;

import org.bukkit.entity.Player;

/**
 * Temporary stub for Menu during migration
 */
public class Menu {
    private final int rows;
    private final String title;
    
    public Menu(int rows, String title) {
        this.rows = rows;
        this.title = title;
    }
    
    public Menu item(int slot, Item item) {
        // TODO: Implement with LoLib GUI
        return this;
    }
    
    public Menu distributeRowsEvenly() {
        // TODO: Implement
        return this;
    }
    
    public void open(Player player) {
        // TODO: Implement with LoLib GUI
        player.sendMessage("§cМеню временно недоступно во время миграции");
    }
}
