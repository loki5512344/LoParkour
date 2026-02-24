package dev.loki.loparkour.mode;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HardcoreMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "hardcore";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.hardcore");
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
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof HardcoreMode) {
            return;
        }

        player.closeInventory();

        Session.create(session -> new HardcoreGenerator(session), null, null, player);
    }

    private static class HardcoreGenerator extends ParkourGenerator {

        public HardcoreGenerator(@NotNull Session session) {
            super(session);
        }

        @Override
        protected void fall() {
            boolean resetRewards = Config.CONFIG.getBoolean("modes.hardcore.reset-rewards");
            
            if (resetRewards) {
                player.sendTranslated("modes.hardcore.rewards-lost");
            }

            player.sendTranslated("modes.hardcore.full-reset");
            
            super.fall();
        }

        @Override
        public Mode getMode() {
            return Modes.HARDCORE;
        }
    }
}
