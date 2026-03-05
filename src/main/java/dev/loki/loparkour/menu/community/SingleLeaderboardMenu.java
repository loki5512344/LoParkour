package dev.loki.loparkour.menu.community;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.menu.LPMenu;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.util.ColorUtil;
import dev.lolib.gui.ScrollableGUI;
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

    public void open(@NotNull Player player, @NotNull Mode mode, @NotNull Leaderboard.Sort sort) {
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
                    if (meta != null) { meta.setOwningPlayer(op); skull.setItemMeta(meta); }
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

        ScrollableGUI gui = ScrollableGUI.create()
                .title(ColorUtil.color(title))
                .rows(3)
                .scrollUpButton(backItem(), 0)
                .scrollDownButton(nextItem(), 8);

        for (ItemStack item : items) {
            gui = gui.addItem(item, e -> { /* view only */ });
        }

        gui.open(player);
    }

    @Override
    public void open(@NotNull Player player) { /* use open(player, mode, sort) */ }
}
