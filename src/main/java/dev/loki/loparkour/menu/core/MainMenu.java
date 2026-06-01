package dev.loki.loparkour.menu.core;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        String locale = locale(player);
        String title = Locales.getString(locale, "main.name");

        List<ItemStack> visibleItems = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        if (ParkourOption.PLAY.mayPerform(player) && Config.CONFIG.getBoolean("joining")) {
            ItemStack item = localeItem(player, "play.item");
            if (item != null) {
                visibleItems.add(item);
                actions.add(() -> Menus.PLAY.open(player));
            }
        }
        if (ParkourOption.COMMUNITY.mayPerform(player)) {
            ItemStack item = localeItem(player, "community.item");
            if (item != null) {
                visibleItems.add(item);
                actions.add(() -> Menus.COMMUNITY.open(player));
            }
        }
        if (ParkourOption.SETTINGS.mayPerform(player) && ParkourUser.isUser(player)) {
            ItemStack item = localeItem(player, "settings.item");
            if (item != null) {
                visibleItems.add(item);
                actions.add(() -> Menus.SETTINGS.open(player));
            }
        }
        if (ParkourOption.LOBBY.mayPerform(player) && ParkourUser.isUser(player)) {
            ItemStack item = localeItem(player, "lobby.item");
            if (item != null) {
                visibleItems.add(item);
                actions.add(() -> Menus.LOBBY.open(player));
            }
        }
        if (ParkourOption.QUIT.mayPerform(player) && ParkourUser.isUser(player)) {
            ItemStack item = localeItem(player, "other.quit");
            if (item != null) {
                visibleItems.add(item);
                actions.add(() -> ParkourUser.leave(player));
            }
        }

        int count = visibleItems.size();
        if (count == 0) {
            Locales.send(player, "other.no_menu_items");
            return;
        }
        
        int[] slots = distributeEvenly(count, 9, 17);

        InventoryGUI gui = baseGui(title, 3);

        for (int i = 0; i < count; i++) {
            final int idx = i;
            gui = gui.setItem(slots[i], visibleItems.get(i), e -> actions.get(idx).run());
        }

        gui.open(player);
    }
}
