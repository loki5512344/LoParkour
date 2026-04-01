package dev.loki.loparkour.mode;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;

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
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.default");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage("Joining is currently disabled.");
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
