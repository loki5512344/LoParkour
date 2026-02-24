package dev.loki.loparkour.menu.play;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.DynamicMenu;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.efnilite.vilib.inventory.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The menu where players can join modes
 */
public class PlayMenu extends DynamicMenu {

    public PlayMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "play.single.item")
                        .click(event -> Menus.SINGLE.open(event.getPlayer())),
                ParkourOption.SINGLE::mayPerform);

        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "other.iep")
                        .click(event -> event.getPlayer().performCommand("iep play")),
                player -> Bukkit.getPluginManager().isPluginEnabled("IEP"));

        registerMainItem(1, 6, (player, user) -> Locales.getItem(player, "play.spectator.item")
                        .click(event -> Menus.SPECTATOR.open(event.getPlayer())),
                ParkourOption.SPECTATOR::mayPerform);

        registerMainItem(2, 0, (player, user) -> Locales.getItem(player, "other.close")
                        .click(event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "play.name"))
                .distributeRowsEvenly());
    }
}
