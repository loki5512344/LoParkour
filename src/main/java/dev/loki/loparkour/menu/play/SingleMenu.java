package dev.loki.loparkour.menu.play;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.LPMenu;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.MultiMode;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.util.ColorUtil;
import dev.lolib.gui.ScrollableGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SingleMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        String locale = locale(player);

        List<Mode> availableModes = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();

        for (Mode mode : Registry.getModes()) {
            boolean blocked = Config.CONFIG.getBoolean("permissions.enabled")
                    && !player.hasPermission("LoParkour.gamemode." + mode.getName());
            if (blocked || mode instanceof MultiMode) continue;
            var item = mode.getItem(locale);
            if (item == null) continue;
            availableModes.add(mode);
            items.add(item.build());
        }

        if (availableModes.size() == 1) {
            availableModes.get(0).create(player);
            return;
        }

        String title = Locales.getString(locale, "play.single.name");

        ScrollableGUI gui = ScrollableGUI.create()
                .title(ColorUtil.color(title))
                .rows(3)
                .scrollUpButton(backItem(), 0)
                .scrollDownButton(nextItem(), 8);

        for (int i = 0; i < items.size(); i++) {
            final Mode mode = availableModes.get(i);
            gui = gui.addItem(items.get(i), e -> {
                // Prevent multiple clicks - check if player is already in parkour
                if (ParkourUser.getUser(player) != null) {
                    return; // Already in parkour, ignore click
                }
                
                player.closeInventory();
                mode.create(player);
            });
        }

        gui.open(player);
    }
}
