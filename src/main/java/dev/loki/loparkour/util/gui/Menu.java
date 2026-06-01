package dev.loki.loparkour.util.gui;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.util.item.Item;
import org.bukkit.entity.Player;

/**
 * Legacy menu stub — real menus use LoLib {@link dev.lolib.gui.InventoryGUI}.
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
        Locales.send(player, "other.menu_unavailable", title, rows);
    }
}
