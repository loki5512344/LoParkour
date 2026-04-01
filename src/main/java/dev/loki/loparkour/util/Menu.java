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
        return this;
    }
    
    public Menu distributeRowsEvenly() {
        return this;
    }
    
    public void open(Player player) {
        player.sendMessage("§c" + title + " (" + rows + " рядов) — меню временно недоступно (миграция GUI).");
    }
}
