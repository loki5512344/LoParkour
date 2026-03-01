package dev.loki.loparkour.menu.play;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.MultiMode;
import dev.loki.loparkour.player.ParkourUser;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The menu to select single player modes
 */
public class SingleMenu {

    // TODO: Migrate to LoLib GUI system
    /*
    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        List<Mode> modes = Registry.getModes();

        List<MenuItem> items = new ArrayList<>();
        List<Mode> modeSet = new ArrayList<>();
        for (Mode mode : modes) {
            boolean permissions = Config.CONFIG.getBoolean("permissions.enabled") && !player.hasPermission("LoParkour.gamemode." + mode.getName());

            Item item = mode.getItem(locale);

            if (permissions || mode instanceof MultiMode || item == null) {
                continue;
            }

            modeSet.add(mode);

            items.add(item.clone().click(event -> {
                if (user == null || Duration.between(user.joined, Instant.now()).toSeconds() > 3) {
                    mode.create(player);
                }
            }));
        }

        if (modeSet.size() == 1) {
            modeSet.get(0).create(player);
            return;
        }
        // PagedMenu mode = new PagedMenu(3, Locales.getString(player, "play.single.name"));
        // mode.displayRows(0, 1)
        // .addToDisplay(items)
        // .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>»").click(event -> mode.page(1)))
        // .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>«").click(event -> mode.page(-1)))
        // .item(22, Locales.getItem(player, "other.close").click(event -> Menus.PLAY.open(event.getPlayer())))
        // .open(player);
    }
    */

}
