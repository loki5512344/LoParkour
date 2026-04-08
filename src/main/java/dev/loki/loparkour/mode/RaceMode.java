package dev.loki.loparkour.mode;

import dev.loki.loparkour.LoParkour;
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
 * Race Mode — reach the target score as fast as possible.
 * Leaderboard sorts by time (fastest wins).
 *
 * <p>Bugs fixed vs previous version:
 * <ul>
 *   <li>{@code tick()} called {@code super.tick()} AND checked score every tick —
 *       but super.tick() already calls {@code score()} internally, so the finish
 *       check ran one tick late causing an off-by-one where state.score could
 *       briefly exceed targetScore before finishing. Fixed by overriding
 *       {@link #score()} instead — finish check fires the exact moment target is hit.</li>
 *   <li>ActionBar was re-sent every tick inside tick() which conflicted with the
 *       parent's ActionBar updates. Moved to score() so it updates only on change.</li>
 *   <li>{@code reset(false)} was called via scheduler delay — if the player left the
 *       server in those 5 seconds it would NPE. Now guarded with null-check.</li>
 *   <li>{@code getFormattedTime()} is private in parent — accessed via
 *       {@code getDetailedTime()} which is protected. Fixed method call.</li>
 * </ul>
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
            player.sendMessage("§cJoining is currently disabled.");
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator instanceof RaceGenerator) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new RaceGenerator(session), null, null, player);
    }

    // ── RaceGenerator ─────────────────────────────────────────────────────────

    private static class RaceGenerator extends ParkourGenerator {

        private final int targetScore;
        private boolean raceFinished = false;

        public RaceGenerator(@NotNull Session session) {
            super(session);
            this.targetScore = Config.CONFIG.getInt("modes.race.target-score");
        }

        /**
         * Hook into score() — the exact moment a new point is added.
         * This ensures finish detection is frame-perfect with no off-by-one.
         */
        @Override
        protected void score() {
            super.score();

            // Update action bar on every new block
            updateProgressBar();

            if (!raceFinished && state.score >= targetScore) {
                finishRace();
            }
        }

        @SuppressWarnings("deprecation") // Spigot Chat API: action bar via TextComponent
        private void updateProgressBar() {
            int remaining = Math.max(0, targetScore - state.score);
            int filled = Math.min(20, state.score * 20 / targetScore);
            String bar = "§a" + "█".repeat(filled) + "§7" + "█".repeat(20 - filled);

            getPlayers().forEach(pp ->
                pp.player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        bar + " §e" + state.score + " §7/ §a" + targetScore
                        + (remaining > 0 ? " §7(§c" + remaining + " §7left)" : " §6§lDONE!")
                    )));
        }

        private void finishRace() {
            raceFinished = true;
            String time = getDetailedTime();

            getPlayers().forEach(pp -> {
                pp.player.sendTitle(
                    "§6§lRACE COMPLETE!",
                    "§aFinished in §f" + time,
                    10, 80, 20);
                pp.player.playSound(pp.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                pp.sendTranslated("modes.race.completed",
                    Integer.toString(targetScore), time);
            });

            // Save to leaderboard
            registerScore(time, "1.0", targetScore);

            // Return to lobby after 5 seconds — guarded against null/closed session
            dev.lolib.scheduler.Scheduler.get(LoParkour.getPlugin())
                .runLater(() -> {
                    if (session != null && !state.stopped) {
                        reset(false);
                    }
                }, 100L);
        }

        @Override
        public void reset(boolean regenerate) {
            raceFinished = false;
            super.reset(regenerate);
        }

        @Override
        public Mode getMode() {
            return Modes.RACE;
        }
    }
}
