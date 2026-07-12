package dev.loki.loparkour.menu.play;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.loki.loparkour.session.core.Session;
import dev.loki.loparkour.world.core.Divider;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SpectatorMenu extends LPMenu {

    private static final int ITEMS_PER_PAGE = 7; // Slots 10-16 (middle row)
    private int currentPage = 0;

    @Override
    public void open(@NotNull Player player) {
        open(player, 0);
    }
    
    public void open(@NotNull Player player, int page) {
        this.currentPage = page;
        String locale = locale(player);
        ParkourUser user = ParkourUser.getUser(player);
        String title = Locales.getString(locale, "play.spectator.name");

        List<Session> sessions = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();

        for (Session session : Divider.sections.keySet()) {
            if (!session.isAcceptingSpectators()) continue;
            if (user != null && session == user.session) continue;
            if (session.getPlayers().isEmpty()) continue;

            var pp = session.getPlayers().get(0);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            if (!ParkourUser.isBedrockPlayer(player)
                    && pp.getName() != null && !pp.getName().startsWith(".")) {
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(pp.player);
                    skull.setItemMeta(meta);
                }
            }
            sessions.add(session);
            items.add(skull);
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        InventoryGUI gui = baseGui(title, 3);

        // Add items for current page
        for (int i = startIndex; i < endIndex; i++) {
            final Session session = sessions.get(i);
            int slot = 10 + (i - startIndex); // Slots 10-16
            gui = gui.setItem(slot, items.get(i), e -> Modes.SPECTATOR.create(player, session));
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
