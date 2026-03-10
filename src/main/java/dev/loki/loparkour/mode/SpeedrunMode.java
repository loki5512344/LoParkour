package dev.loki.loparkour.mode;

import dev.loki.loparkour.util.Item;

import dev.lolib.scheduler.Scheduler;
import dev.lolib.scheduler.ScheduledTask;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SpeedrunMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "speedrun";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.speedrun");
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
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof SpeedrunMode) {
            return;
        }

        player.closeInventory();

        Session.create(session -> new SpeedrunGenerator(session), null, null, player);
    }

    public static class SpeedrunGenerator extends ParkourGenerator {
        private final Map<Block, Long> blockTimestamps = new HashMap<>();
        private final Map<Block, ScheduledTask> scheduledTasks = new HashMap<>();

        public SpeedrunGenerator(@NotNull Session session) {
            super(session);
            
            // Notify player
            for (ParkourPlayer pp : session.getPlayers()) {
                pp.player.sendTitle("§c§lSPEEDRUN MODE", "§7Blocks disappear after you step on them!", 10, 70, 20);
                pp.player.sendMessage("§c§lSpeedrun Mode §7activated! Don't look back!");
            }
        }

        public void onBlockTouch(@NotNull Block block) {
            if (!blockTimestamps.containsKey(block)) {
                blockTimestamps.put(block, System.currentTimeMillis());
                scheduleBlockRemoval(block);
            }
        }

        private void scheduleBlockRemoval(@NotNull Block block) {
            double blockLifetime = Config.CONFIG.getDouble("modes.speedrun.block-lifetime");
            double warningTime = Config.CONFIG.getDouble("modes.speedrun.warning-time");

            long removalTime = (long) (blockLifetime * 1000);
            long warningTimeMs = (long) (warningTime * 1000);

            ScheduledTask warningTask = Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
                    if (block.getType() != Material.AIR) {
                        player.player.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                        player.player.spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.01);
                    }
                }, warningTimeMs / 50);

            ScheduledTask removalTask = Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR);
                        blockTimestamps.remove(block);
                        scheduledTasks.remove(block);
                    }
                }, removalTime / 50);

            scheduledTasks.put(block, removalTask);
        }

        @Override
        public void reset(boolean regenerate) {
            // Cancel all scheduled tasks
            for (ScheduledTask task : scheduledTasks.values()) {
                if (task != null) {
                    task.cancel();
                }
            }
            scheduledTasks.clear();
            blockTimestamps.clear();
            
            super.reset(regenerate);
        }

        @Override
        public Mode getMode() {
            return Modes.SPEEDRUN;
        }
    }
}
