package dev.loki.loparkour.menu.lobby;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.DynamicMenu;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.session.Session;
import org.bukkit.entity.Player;

import java.util.List;

public class LobbyMenu extends DynamicMenu {

    // TODO: Migrate to LoLib GUI system
    /*
    public LobbyMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "lobby.player_management.item").click(event -> Menus.PLAYER_MANAGEMENT.open(player)), player -> {
            ParkourUser user = ParkourUser.getUser(player);

            return ParkourOption.PLAYER_MANAGEMENT.mayPerform(player) && user instanceof ParkourPlayer && user.session.getPlayers().get(0) == user;
        });

        registerMainItem(1, 1, (player, user) -> {
            if (user == null) {
                return null;
            }

            List<String> values = Locales.getStringList(user.locale, "lobby.visibility.values");

            return new SliderItem().initial(switch (user.session.getVisibility()) {
                case PUBLIC -> 0;
                case ID_ONLY -> 1;
                case PRIVATE -> 2;
            }).add(0, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.setVisibility(Session.Visibility.PUBLIC);
                }

                return true;
            }).add(1, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // id only
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.setVisibility(Session.Visibility.ID_ONLY);
                }

                return true;
            }).add(2, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(0))), event -> { // private
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.setVisibility(Session.Visibility.PRIVATE);
                }

                return true;
            });
        }, player -> {
            ParkourUser user = ParkourUser.getUser(player);

            return ParkourOption.VISIBILITY.mayPerform(player) && user instanceof ParkourPlayer && user.session.getPlayers().get(0) == user;
        });

        registerMainItem(2, 10, (player, user) -> Locales.getItem(player, "other.close").click(event -> event.getPlayer().closeInventory()), player -> true);
    }
    */

    /**
     * Opens the main menu.
     *
     * @param player The player to open the menu to
     */
    // TODO: Migrate to LoLib GUI system
    /*
    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "lobby.name"))
                .distributeRowsEvenly());
    }
    */
}
