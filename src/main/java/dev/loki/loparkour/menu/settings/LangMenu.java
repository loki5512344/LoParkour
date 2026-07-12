package dev.loki.loparkour.menu.settings;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.util.text.ColorUtil;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LangMenu extends LPMenu {

    public void open(@NotNull ParkourPlayer user) {
        if (user == null) {
            return;
        }
        open(user.player);
    }

    @Override
    public void open(@NotNull Player player) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp == null) {
            return;
        }

        String locale = pp.locale;
        String title = Locales.getString(locale, "settings.lang.name");

        List<String> langs = new ArrayList<>(Locales.getLocaleKeys());

        InventoryGUI gui = baseGui(title, 3);

        for (int i = 0; i < Math.min(langs.size(), 7); i++) {
            final String lang = langs.get(i);
            String langName = Locales.getString(lang, "name");
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.color("&#238681&l" + langName));
                item.setItemMeta(meta);
            }
            gui = gui.setItem(10 + i, item, e -> {
                pp.locale = lang;
                pp.updateHotbar(); // Update hotbar with new language
                Menus.SETTINGS.open(player);
            });
        }

        gui.open(player);
    }
}
