package dev.loki.loparkour.menu.play;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.LPMenu;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        String title = Locales.getString(locale(player), "play.name");

        baseGui(title, 3)
                .setItem(11, localeItem(player, "play.single.item"),
                        e -> { if (ParkourOption.SINGLE.mayPerform(player)) Menus.SINGLE.open(player); })
                .setItem(15, localeItem(player, "play.spectator.item"),
                        e -> { if (ParkourOption.SPECTATOR.mayPerform(player)) Menus.SPECTATOR.open(player); })
                .setItem(22, closeButton(player), e -> player.closeInventory())
                .open(player);
    }
}
