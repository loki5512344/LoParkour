package dev.loki.loparkour.mode.impl;
import dev.loki.loparkour.mode.elytra.ElytraGenerator;
import dev.loki.loparkour.mode.base.ModeMessages;
import dev.loki.loparkour.mode.base.Mode;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.session.core.Session;
import dev.loki.loparkour.util.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Elytra Parkour Mode.
 * Players fly through vertical rings generated ahead along a curved aerial path.
 * No blocks on the ground — pure flight mechanics.
 */
public class ElytraMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "elytra";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return Locales.getItem(locale, "modes.elytra");
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
        if (pp != null && pp.session.generator instanceof ElytraGenerator) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new ElytraGenerator(session), null, null, player);
    }
}
