package dev.loki.loparkour.util.gui;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.util.item.Item;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Legacy paged menu stub — real menus use LoLib GUI.
 */
public class PagedMenu {
    private final int rows;
    private final String title;
    private List<Item> items = List.of();

    public PagedMenu(int rows, String title) {
        this.rows = rows;
        this.title = title;
    }

    public PagedMenu addToDisplay(List<Item> items) {
        this.items = items;
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

    public void open(Player player) {
        Locales.send(player, "other.paged_menu_unavailable", title, rows, items.size());
    }
}
