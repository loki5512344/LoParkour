package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.util.text.ColorUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Elytra mode action bar display.
 */
public final class ElytraHud {

    private ElytraHud() {
    }

    @SuppressWarnings("deprecation") // Spigot action bar via TextComponent
    public static void sendActionBar(
            @NotNull ParkourPlayer player,
            int ringIndex,
            int totalRings,
            int score) {
        String message = Locales.getString(player.locale, "modes.elytra.action_bar")
                .formatted(ringIndex + 1, totalRings, score);

        player.player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtil.color(message)));
    }
}
