package dev.loki.loparkour.mode.impl;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.mode.base.ModeMessages;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.session.core.Session;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default parkour mode
 */
public class DefaultMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "default";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.item.Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.default");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!ModeMessages.checkJoiningEnabled(player)) {
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof DefaultMode) {
            return;
        }

        player.closeInventory();

        Session.create(ParkourGenerator::new, null, null, player);
    }
}
