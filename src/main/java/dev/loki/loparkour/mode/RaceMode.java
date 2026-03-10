package dev.loki.loparkour.mode;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Race Mode - First to reach target score wins
 */
public class RaceMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.TIME);

    @Override
    @NotNull
    public String getName() {
        return "race";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return Locales.getItem(locale, "modes.race");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage(Locales.getString(player, "other.joining_disabled"));
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof RaceMode) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new RaceGenerator(session), null, null, player);
    }

    private static class RaceGenerator extends ParkourGenerator {
        private final int targetScore;
        private boolean raceFinished = false;

        public RaceGenerator(@NotNull Session session) {
            super(session);
            this.targetScore = Config.CONFIG.getInt("modes.race.target-score");
            
            // Notify players
            for (ParkourPlayer pp : session.getPlayers()) {
                pp.player.sendTitle("§6§lRACE MODE", "§7First to " + targetScore + " points wins!", 10, 70, 20);
                pp.player.sendMessage("§6§lRace Mode §7activated! Target: §e" + targetScore + " §7points.");
            }
        }

        @Override
        public void tick() {
            super.tick();
            
            if (raceFinished) return;
            
            // Check if target reached
            if (state.score >= targetScore) {
                finishRace();
            }
            
            // Display progress in ActionBar
            for (ParkourPlayer pp : getPlayers()) {
                int remaining = targetScore - state.score;
                String progress = "§e" + state.score + " §7/ §a" + targetScore + " §7(§c" + remaining + " §7left)";
                pp.player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(progress)
                );
            }
        }

        private void finishRace() {
            raceFinished = true;
            
            for (ParkourPlayer pp : getPlayers()) {
                pp.player.sendTitle("§6§lRACE COMPLETE!", "§aFinished in " + getFormattedTime(), 10, 70, 20);
                pp.player.playSound(pp.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                pp.sendTranslated("modes.race.completed", Integer.toString(targetScore), getFormattedTime());
            }
            
            // Register time-based score
            registerScore(getDetailedTime(), Double.toString(getDifficultyScore()).substring(0, 3), targetScore);
            
            // Stop generator after 5 seconds
            dev.lolib.scheduler.Scheduler.get(dev.loki.loparkour.LoParkour.getPlugin())
                .runLater(() -> reset(false), 100);
        }

        @Override
        public Mode getMode() {
            return Modes.RACE;
        }
    }
}
