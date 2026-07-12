package dev.loki.loparkour.menu.play;

import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SingleMenu extends LPMenu {

    private static final int ITEMS_PER_PAGE = 7; // Slots 10-16 (middle row)
    private int currentPage = 0;

    @Override
    public void open(@NotNull Player player) {
        open(player, 0);
    }
    
    public void open(@NotNull Player player, int page) {
        this.currentPage = page;
        String locale = locale(player);

        List<Mode> availableModes = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();

        for (Mode mode : Registry.getModes()) {
            // Skip spectator mode in single menu
            if ("spectator".equals(mode.getName())) {
                continue;
            }

            // Check if mode is enabled in config
            if (!Config.CONFIG.getBoolean("modes." + mode.getName() + ".enabled", true)) {
                continue;
            }

            boolean blocked = Config.CONFIG.getBoolean("permissions.enabled")
                    && !player.hasPermission("LoParkour.gamemode." + mode.getName());
            if (blocked) {
                continue;
            }

            var item = mode.getItem(locale);
            if (item == null) {
                continue;
            }
            
            availableModes.add(mode);
            items.add(item.build());
        }

        if (availableModes.size() == 1) {
            availableModes.get(0).create(player);
            return;
        }

        String title = Locales.getString(locale, "play.single.name");

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        InventoryGUI gui = baseGui(title, 3);

        // Add items for current page
        for (int i = startIndex; i < endIndex; i++) {
            final Mode mode = availableModes.get(i);
            int slot = 10 + (i - startIndex); // Slots 10-16
            gui = gui.setItem(slot, items.get(i), e -> {
                // Prevent multiple clicks - check if player is already in parkour
                if (ParkourUser.getUser(player) != null) {
                    return; // Already in parkour, ignore click
                }
                
                player.closeInventory();
                mode.create(player);
            });
        }

        // Add navigation buttons
        if (currentPage > 0) {
            // Previous page button
            gui = gui.setItem(0, backItem(player), e -> open(player, currentPage - 1));
        }
        
        if (currentPage < totalPages - 1) {
            // Next page button
            gui = gui.setItem(8, nextItem(player), e -> open(player, currentPage + 1));
        }

        gui.open(player);
    }
}
