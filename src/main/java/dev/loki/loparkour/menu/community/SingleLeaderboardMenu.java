package dev.loki.loparkour.menu.community;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.leaderboard.model.Score;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SingleLeaderboardMenu extends LPMenu {

    private static final int ITEMS_PER_PAGE = 7; // Slots 10-16 (middle row)
    private int currentPage = 0;

    public void open(@NotNull Player player, @NotNull Mode mode, @NotNull Leaderboard.Sort sort) {
        open(player, mode, sort, 0);
    }
    
    public void open(@NotNull Player player, @NotNull Mode mode, @NotNull Leaderboard.Sort sort, int page) {
        this.currentPage = page;
        Leaderboard leaderboard = mode.getLeaderboard();
        if (leaderboard == null) return;

        String locale = locale(player);
        String title = Locales.getString(locale, ParkourOption.LEADERBOARDS.path + ".name");

        List<ItemStack> items = new ArrayList<>();
        int rank = 0;

        for (Map.Entry<UUID, Score> entry : leaderboard.sort(sort).entrySet()) {
            rank++;
            Score score = entry.getValue();
            if (score == null) continue;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            if (rank <= 20 && !ParkourUser.isBedrockPlayer(player)) {
                OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
                if (op.getName() != null && !op.getName().startsWith(".")) {
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    if (meta != null) {
                        meta.setOwningPlayer(op);
                        skull.setItemMeta(meta);
                    }
                }
            }

            var base = Locales.getItem(locale, ParkourOption.LEADERBOARDS.path + ".head");
            int r = rank;
            base.modifyName(n -> n.replace("%r", String.valueOf(r))
                    .replace("%s", String.valueOf(score.score()))
                    .replace("%p", score.name())
                    .replace("%t", score.time())
                    .replace("%d", score.difficulty()))
               .modifyLore(l -> l.replace("%r", String.valueOf(r))
                    .replace("%s", String.valueOf(score.score()))
                    .replace("%p", score.name())
                    .replace("%t", score.time())
                    .replace("%d", score.difficulty()));
            ItemMeta built = base.build().getItemMeta();
            if (built != null) skull.setItemMeta(built);

            items.add(skull);
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        InventoryGUI gui = baseGui(title, 3);

        // Add items for current page
        for (int i = startIndex; i < endIndex; i++) {
            int slot = 10 + (i - startIndex); // Slots 10-16
            gui = gui.setItem(slot, items.get(i), e -> { /* view only */ });
        }

        // Add navigation buttons
        if (currentPage > 0) {
            // Previous page button
            gui = gui.setItem(0, backItem(player), e -> open(player, mode, sort, currentPage - 1));
        }
        
        if (currentPage < totalPages - 1) {
            // Next page button
            gui = gui.setItem(8, nextItem(player), e -> open(player, mode, sort, currentPage + 1));
        }

        gui.open(player);
    }

    @Override
    public void open(@NotNull Player player) { /* use open(player, mode, sort) */ }
}
