package dev.loki.loparkour.mode.impl;
import dev.loki.loparkour.mode.base.ModeMessages;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.mode.base.Modes;

import dev.loki.loparkour.util.item.Item;
import dev.lolib.scheduler.Scheduler;
import dev.lolib.scheduler.ScheduledTask;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.session.core.Session;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Speedrun Mode — blocks disappear a short time after the player lands on them.
 *
 * <p>Bugs fixed vs previous version:
 * <ul>
 *   <li>{@code onBlockTouch()} was never called from anywhere — blocks never disappeared.
 *       Fixed by hooking into {@link #score()} which fires exactly when the player
 *       steps onto a new block for the first time.</li>
 *   <li>Warning task reference was stored in a throwaway local, not in scheduledTasks,
 *       so it could never be cancelled on reset.</li>
 *   <li>{@code removalTime / 50} used integer division that truncated sub-second values
 *       to 0 ticks when blockLifetime < 0.05s. Now uses Math.max(1, ...).</li>
 * </ul>
 */
public class SpeedrunMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "speedrun";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.speedrun");
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
        if (pp != null && pp.session.generator instanceof SpeedrunGenerator) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new SpeedrunGenerator(session), null, null, player);
    }

    // ── SpeedrunGenerator ─────────────────────────────────────────────────────

    private static class SpeedrunGenerator extends ParkourGenerator {

        private static final int TICKS_PER_SECOND = 20;

        /** Tracks scheduled tasks per block. Key = block, value = [warningTask, removalTask]. */
        private final Map<Block, ScheduledTask[]> scheduledTasks = new HashMap<>();

        SpeedrunGenerator(@NotNull Session session) {
            super(session);
        }

        /**
         * Called every time the player scores a point (= lands on a new block).
         * We hook here instead of a separate onBlockTouch() because score() is the
         * canonical place where "player stepped on block N" is detected.
         */
        @Override
        protected void score() {
            super.score();

            // The block the player just landed on is the previous-to-last in history
            // (history.size()-1 is the leading block, size()-2 is where player stands)
            int histSize = state.history.size();
            if (histSize < 2) {
                return;
            }

            Block stepped = state.history.get(histSize - 2);
            if (stepped.getType() == Material.AIR) {
                return;
            }
            if (scheduledTasks.containsKey(stepped)) {
                return;
            }

            scheduleBlockRemoval(stepped);
        }

        private void scheduleBlockRemoval(@NotNull Block block) {
            double blockLifetime = Config.CONFIG.getDouble("modes.speedrun.block-lifetime");
            double warningTime   = Config.CONFIG.getDouble("modes.speedrun.warning-time");

            // Convert seconds → ticks, minimum 1 tick
            long removalTicks = Math.max(1, Math.round(blockLifetime * TICKS_PER_SECOND));
            long warningTicks = Math.max(1, Math.round(warningTime * TICKS_PER_SECOND));

            ScheduledTask warningTask = Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
                if (block.getType() != Material.AIR) {
                    for (ParkourPlayer pp : getPlayers()) {
                        pp.player.playSound(block.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                        pp.player.spawnParticle(Particle.FLAME,
                            block.getLocation().add(0.5, 1, 0.5), 8, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }, warningTicks);

            ScheduledTask removalTask = Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
                scheduledTasks.remove(block);
            }, removalTicks);

            // Store both so we can cancel them on reset
            scheduledTasks.put(block, new ScheduledTask[]{warningTask, removalTask});
        }

        @Override
        public void reset(boolean regenerate) {
            // Cancel every pending warning and removal task
            scheduledTasks.values().forEach(tasks -> {
                for (ScheduledTask t : tasks) {
                    if (t != null) {
                        t.cancel();
                    }
                }
            });
            scheduledTasks.clear();

            super.reset(regenerate);
        }

        @Override
        public Mode getMode() {
            return Modes.SPEEDRUN;
        }
    }
}
