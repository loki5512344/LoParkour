package dev.loki.loparkour.menu;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import org.bukkit.entity.Player;

public class MainMenu extends DynamicMenu {

    // TODO: Migrate to LoLib GUI system
    /*
    public MainMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "play.item").click(event -> Menus.PLAY.open(event.getPlayer())), player -> ParkourOption.PLAY.mayPerform(player) && Config.CONFIG.getBoolean("joining"));
        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "community.item").click(event -> Menus.COMMUNITY.open(event.getPlayer())), ParkourOption.COMMUNITY::mayPerform);
        registerMainItem(1, 2, (player, user) -> Locales.getItem(player, "settings.item").click(event -> Menus.SETTINGS.open(event.getPlayer())), player -> ParkourOption.SETTINGS.mayPerform(player) && ParkourUser.isUser(player));
        registerMainItem(1, 3, (player, user) -> Locales.getItem(player, "lobby.item").click(event -> Menus.LOBBY.open(event.getPlayer())), player -> ParkourOption.LOBBY.mayPerform(player) && ParkourUser.isUser(player));
        registerMainItem(1, 4, (player, user) -> Locales.getItem(player, "other.quit").click(event -> ParkourPlayer.leave(player)), player -> ParkourOption.QUIT.mayPerform(player) && ParkourUser.isUser(player));
    }
    */

    // TODO: Migrate to LoLib GUI system
    /*
    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "main.name"))
                .distributeRowsEvenly());
    }
    */
}
