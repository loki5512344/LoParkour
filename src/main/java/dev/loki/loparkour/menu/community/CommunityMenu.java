package dev.loki.loparkour.menu.community;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.menu.core.ParkourOption;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommunityMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        String title = Locales.getString(locale(player), "community.name");

        baseGui(title, 3)
                .setItem(13, localeItem(player, "community.leaderboards.item"),
                        e -> { if (ParkourOption.LEADERBOARDS.mayPerform(player)) Menus.LEADERBOARDS.open(player); })
                .setItem(22, closeButton(player), e -> player.closeInventory())
                .open(player);
    }
}
