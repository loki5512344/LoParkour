package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.player.core.ParkourPlayer;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

/**
 * Ring pass feedback and fall messages for elytra mode.
 */
public final class ElytraScoring {

    private ElytraScoring() {
    }

    public static void onRingPassed(
            @NotNull ParkourGenerator generator,
            @NotNull ParkourPlayer player,
            @NotNull ElytraRing ring,
            @NotNull ElytraPhysics physics) {
        boolean centered = physics.isPlayerCentered(player.player.getLocation(), ring);
        int points = centered ? 10 : 5;

        generator.state.score += points;
        player.player.playSound(player.player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        String key = centered ? "modes.elytra.ring_perfect" : "modes.elytra.ring_good";
        Locales.send(player.player, key, points);
    }

    public static void notifyFall(@NotNull ParkourPlayer player, @NotNull String reason) {
        Locales.send(player.player, "modes.elytra.fell", reason);
    }
}
