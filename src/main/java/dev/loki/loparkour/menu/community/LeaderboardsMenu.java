package dev.loki.loparkour.menu.community;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.menu.LPMenu;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardsMenu extends LPMenu {

    private static final int ITEMS_PER_PAGE = 7; // Slots 10-16 (middle row)
    private int currentPage = 0;

    @Override
    public void open(@NotNull Player player) {
        open(player, 0);
    }
    
    public void open(@NotNull Player player, int page) {
        this.currentPage = page;
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

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        InventoryGUI gui = baseGui(title, 3);

        // Add items for current page
        for (int i = startIndex; i < endIndex; i++) {
            final Mode mode = modes.get(i);
            int slot = 10 + (i - startIndex); // Slots 10-16
            gui = gui.setItem(slot, items.get(i),
                    e -> Menus.SINGLE_LEADERBOARD.open(player, mode, Leaderboard.Sort.SCORE));
        }

        // Add navigation buttons
        if (currentPage > 0) {
            // Previous page button
            gui = gui.setItem(0, backItem(), e -> open(player, currentPage - 1));
        }
        
        if (currentPage < totalPages - 1) {
            // Next page button  
            gui = gui.setItem(8, nextItem(), e -> open(player, currentPage + 1));
        }

        gui.open(player);
    }
}
