package dev.loki.loparkour.mode;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.session.Session;

import dev.loki.loparkour.util.ColorUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectatorMode implements Mode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return null;
    }

    @Override
    @Nullable
    public Leaderboard getLeaderboard() {
        return null;
    }

    @Override
    public void create(Player player) {
        Menus.SPECTATOR.open(player);
    }

    public void create(Player player, Session session) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage(ColorUtil.color("<red><bold>Joining is currently disabled."));
            return;
        }
        if (!ParkourOption.SPECTATOR.mayPerform(player)) {
            player.sendMessage(Locales.getString(player, "other.no_do"));
            return;
        }

        ParkourUser user = ParkourUser.getUser(player);
        ParkourSpectator spectator;

        if (session.getPlayers().isEmpty()) {
            return;
        }

        if (user != null) {
            ParkourUser.unregister(user, false, false, false);
            spectator = new ParkourSpectator(player, session, user.previousData);
        } else {
            spectator = new ParkourSpectator(player, session, null);
        }

        session.addSpectators(spectator);
    }
}
