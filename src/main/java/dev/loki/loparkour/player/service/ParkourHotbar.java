package dev.loki.loparkour.player.service;
import dev.loki.loparkour.player.core.ParkourPlayer;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.ParkourOption;
import org.jetbrains.annotations.NotNull;

/**
 * In-session hotbar items (community, settings, lobby, quit).
 */
public final class ParkourHotbar {

    private ParkourHotbar() {
    }

    public static void apply(@NotNull ParkourPlayer player) {
        if (!Config.CONFIG.getBoolean("options.inventory-handling")) {
            return;
        }

        player.player.getInventory().clear();
        fillSlot(player, ParkourOption.COMMUNITY, "options.hotbar-slots.community", "community.item");
        fillSlot(player, ParkourOption.SETTINGS, "options.hotbar-slots.settings", "settings.item");
        fillSlot(player, ParkourOption.LOBBY, "options.hotbar-slots.lobby", "lobby.item");
        fillSlot(player, ParkourOption.QUIT, "options.hotbar-slots.quit", "other.quit");
    }

    private static void fillSlot(
            @NotNull ParkourPlayer player,
            @NotNull ParkourOption option,
            @NotNull String slotPath,
            @NotNull String itemPath) {
        if (!option.mayPerform(player.player)) {
            return;
        }
        int slot = Config.CONFIG.getInt(slotPath);
        player.player.getInventory().setItem(slot, Locales.getItem(player.locale, itemPath).build());
    }
}
