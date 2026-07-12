package dev.loki.loparkour.util.item;

import dev.loki.loparkour.util.text.ColorUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simple ItemStack builder for migration from vilib
 */
public class Item {
    private final ItemStack item;
    private Consumer<org.bukkit.event.inventory.InventoryClickEvent> clickHandler;
    
    public Item(@NotNull Material material, @NotNull String name) {
        this.item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            item.setItemMeta(meta);
        }
    }
    
    public Item lore(@NotNull String... lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtil.color(line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public Item lore(@NotNull List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtil.color(line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public Item material(@NotNull Material material) {
        item.setType(material);
        return this;
    }
    
    public Item click(@NotNull Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }
    
    public Item modifyLore(@NotNull Function<String, String> modifier) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                List<String> newLore = new ArrayList<>();
                for (String line : lore) {
                    newLore.add(modifier.apply(line));
                }
                meta.setLore(newLore);
                item.setItemMeta(meta);
            }
        }
        return this;
    }
    
    public Item modifyName(@NotNull Function<String, String> modifier) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            meta.setDisplayName(modifier.apply(meta.getDisplayName()));
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public Item meta(@NotNull ItemMeta meta) {
        item.setItemMeta(meta);
        return this;
    }
    
    @Override
    @SuppressWarnings("NoClone")
    public Item clone() {
        Item cloned = new Item(item.getType(), "");
        cloned.item.setItemMeta(item.getItemMeta());
        cloned.clickHandler = this.clickHandler;
        return cloned;
    }
    
    public ItemStack build() {
        return item;
    }
    
    public Consumer<org.bukkit.event.inventory.InventoryClickEvent> getClickHandler() {
        return clickHandler;
    }
}
