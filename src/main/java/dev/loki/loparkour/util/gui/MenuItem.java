package dev.loki.loparkour.util.gui;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Menu item holder for migration from vilib
 */
public class MenuItem {
    private final ItemStack item;
    
    public MenuItem(@NotNull ItemStack item) {
        this.item = item;
    }
    
    public ItemStack getItem() {
        return item;
    }
}
