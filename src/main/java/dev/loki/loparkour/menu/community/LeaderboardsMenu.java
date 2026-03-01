package dev.loki.loparkour.menu.community;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.player.ParkourUser;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboards menu
 */
public class LeaderboardsMenu {

    // TODO: Migrate to LoLib GUI system
    /*
    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        PagedMenu menu = new PagedMenu(3, Locales.getString(player, "%s.name".formatted(ParkourOption.LEADERBOARDS.path)));

        Mode latest = null;
        List<MenuItem> items = new ArrayList<>();
        for (Mode mode : Registry.getModes()) {
            Leaderboard leaderboard = mode.getLeaderboard();
            Item item = mode.getItem(locale);

            if (leaderboard == null || item == null) {
                continue;
            }

            items.add(item.clone().click(event -> Menus.SINGLE_LEADERBOARD.open(player, mode, leaderboard.sort)));
            latest = mode;
        }

        if (items.size() == 1) {
            Menus.SINGLE_LEADERBOARD.open(player, latest, Leaderboard.Sort.SCORE);
            return;
        }
        // menu.displayRows(0, 1)
        // .addToDisplay(items)
        // .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>»").click(event -> menu.page(1)))
        // .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>«").click(event -> menu.page(-1)))
        // .item(22, Locales.getItem(player, "other.close").click(event -> Menus.COMMUNITY.open(event.getPlayer())))
        // .open(player);
    }
    */
}
