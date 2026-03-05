package dev.loki.loparkour.menu;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.util.ColorUtil;
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
            visibleItems.add(localeItem(player, "play.item"));
            actions.add(() -> Menus.PLAY.open(player));
        }
        if (ParkourOption.COMMUNITY.mayPerform(player)) {
            visibleItems.add(localeItem(player, "community.item"));
            actions.add(() -> Menus.COMMUNITY.open(player));
        }
        if (ParkourOption.SETTINGS.mayPerform(player) && ParkourUser.isUser(player)) {
            visibleItems.add(localeItem(player, "settings.item"));
            actions.add(() -> Menus.SETTINGS.open(player));
        }
        if (ParkourOption.LOBBY.mayPerform(player) && ParkourUser.isUser(player)) {
            visibleItems.add(localeItem(player, "lobby.item"));
            actions.add(() -> Menus.LOBBY.open(player));
        }
        if (ParkourOption.QUIT.mayPerform(player) && ParkourUser.isUser(player)) {
            visibleItems.add(localeItem(player, "other.quit"));
            actions.add(() -> ParkourUser.leave(player));
        }

        int count = visibleItems.size();
        int[] slots = distributeEvenly(count, 9, 17);

        InventoryGUI gui = baseGui(title, 3);

        for (int i = 0; i < count; i++) {
            final int idx = i;
            gui = gui.setItem(slots[i], visibleItems.get(i), e -> actions.get(idx).run());
        }

        gui.open(player);
    }
}
