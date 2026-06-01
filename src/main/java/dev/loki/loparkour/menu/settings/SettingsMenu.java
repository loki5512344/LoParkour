package dev.loki.loparkour.menu.settings;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SettingsMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        String locale = locale(player);
        String title = Locales.getString(locale, "settings.name");

        InventoryGUI gui = baseGui(title, 3);

        if (ParkourOption.PARKOUR_SETTINGS.mayPerform(player) && ParkourPlayer.isPlayer(player)) {
            gui = gui.setItem(11, localeItem(player, "settings.parkour_settings.item"), e -> {
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) pp.session.generator.menu(pp);
            });
        }

        if (ParkourOption.LANG.mayPerform(player) && ParkourUser.isUser(player)) {
            String langName = Locales.getString(locale, "name");
            gui = gui.setItem(13, localeItem(locale, "settings.lang.item", langName), e -> {
                ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                if (pp != null) Menus.LANG.open(pp);
            });
        }

        gui.setItem(22, closeButton(player), e -> player.closeInventory())
           .open(player);
    }
}
