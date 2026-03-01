package dev.loki.loparkour.menu.community;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.DynamicMenu;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import org.bukkit.entity.Player;

/**
 * The menu for all community-related things
 */
public class CommunityMenu extends DynamicMenu {

    // TODO: Migrate to LoLib GUI system
    /*
    public CommunityMenu() {
        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "community.leaderboards.item").click(event -> Menus.LEADERBOARDS.open(event.getPlayer())), ParkourOption.LEADERBOARDS::mayPerform);
        registerMainItem(2, 10, (player, user) -> Locales.getItem(player, "other.close").click(event -> event.getPlayer().closeInventory()), player -> true);
    }
    */

    // TODO: Migrate to LoLib GUI system
    /*
    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "community.name"))
                .distributeRowsEvenly());
    }
    */
}
