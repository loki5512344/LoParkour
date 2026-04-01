package dev.loki.loparkour.menu;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.util.ColorUtil;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all LoParkour menus.
 *
 * Real LoLib API:
 *   InventoryGUI.create().title(String).rows(n).fill(item).setItem(slot, item, Consumer<InventoryClickEvent>).open(player)
 *   ScrollableGUI.create().title(String).rows(n).addItem(item, Consumer<InventoryClickEvent>).scrollUpButton(item,slot).scrollDownButton(item,slot).open(player)
 *
 *   cancelAllClicks = true by default — items cannot be taken out automatically.
 *   Handlers receive Consumer<InventoryClickEvent>, event is already cancelled before handler runs.
 */
public abstract class LPMenu {

    public abstract void open(@NotNull Player player);

    protected String locale(@NotNull Player player) {
        var user = dev.loki.loparkour.player.ParkourUser.getUser(player);
        String loc = user != null ? user.locale : Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);
        return (loc != null && !loc.isBlank()) ? loc : "en";
    }

    protected ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack backItem() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("<#DE1F1F><bold>Назад"));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack nextItem() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("<#0DCB07><bold>Далее"));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack closeButton(@NotNull Player player) {
        return localeItem(player, "other.close");
    }

    protected ItemStack localeItem(@NotNull Player player, @NotNull String key) {
        return Locales.getItem(player, key).build();
    }

    protected ItemStack localeItem(@NotNull Player player, @NotNull String key, String... replace) {
        return Locales.getItem(player, key, replace).build();
    }

    protected ItemStack localeItem(@NotNull String locale, @NotNull String key) {
        return Locales.getItem(locale, key).build();
    }

    protected ItemStack localeItem(@NotNull String locale, @NotNull String key, String... replace) {
        return Locales.getItem(locale, key, replace).build();
    }

    /**
     * Create a base GUI filled with filler items.
     * Uses .fill() — only fills empty slots, so setItem calls after this override correctly.
     */
    protected InventoryGUI baseGui(String title, int rows) {
        return InventoryGUI.create()
                .title(ColorUtil.color(title))
                .rows(rows)
                .fill(filler());
    }

    protected int[] distributeEvenly(int count, int start, int end) {
        int[] result = new int[count];
        if (count == 0) return result;
        for (int i = 0; i < count; i++) {
            result[i] = start + Math.round((float) i * (end - start) / Math.max(count - 1, 1));
        }
        return result;
    }
}
