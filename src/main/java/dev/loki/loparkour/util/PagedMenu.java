package dev.loki.loparkour.util;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary stub for PagedMenu during migration
 */
public class PagedMenu {
    private final int rows;
    private final String title;
    private final List<MenuItem> items = new ArrayList<>();
    
    public PagedMenu(int rows, String title) {
        this.rows = rows;
        this.title = title;
    }
    
    public PagedMenu addToDisplay(List<Item> items) {
        return this;
    }
    
    public PagedMenu item(int slot, Item item) {
        return this;
    }
    
    public PagedMenu nextPage(int slot, Item item) {
        return this;
    }
    
    public PagedMenu prevPage(int slot, Item item) {
        return this;
    }
    
    public void page(int direction) {
    }
    
    public List<Object> getTotalToDisplay() {
        return new ArrayList<>();
    }
    
    public void open(Player player) {
        player.sendMessage("§c" + title + " (" + rows + " рядов, " + items.size() + " пунктов) — меню временно недоступно.");
    }
}
