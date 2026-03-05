package dev.loki.loparkour.menu.community;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.menu.LPMenu;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.util.ColorUtil;
import dev.lolib.gui.ScrollableGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardsMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        String locale = locale(player);
        String title = Locales.getString(locale, ParkourOption.LEADERBOARDS.path + ".name");

        List<Mode> modes = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();

        for (Mode mode : Registry.getModes()) {
            if (mode.getLeaderboard() == null) continue;
            var item = mode.getItem(locale);
            if (item == null) continue;
            modes.add(mode);
            items.add(item.build());
        }

        if (modes.size() == 1) {
            Menus.SINGLE_LEADERBOARD.open(player, modes.get(0), Leaderboard.Sort.SCORE);
            return;
        }

        ScrollableGUI gui = ScrollableGUI.create()
                .title(ColorUtil.color(title))
                .rows(3)
                .scrollUpButton(backItem(), 0)
                .scrollDownButton(nextItem(), 8);

        for (int i = 0; i < items.size(); i++) {
            final Mode mode = modes.get(i);
            gui = gui.addItem(items.get(i),
                    e -> Menus.SINGLE_LEADERBOARD.open(player, mode, Leaderboard.Sort.SCORE));
        }

        gui.open(player);
    }
}
