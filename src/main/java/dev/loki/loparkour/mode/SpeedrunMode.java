package dev.loki.loparkour.mode;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Task;
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
    private final Map<Block, Long> blockTimestamps = new HashMap<>();

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

    public void onBlockTouch(@NotNull Block block, @NotNull ParkourPlayer player) {
        if (!blockTimestamps.containsKey(block)) {
            blockTimestamps.put(block, System.currentTimeMillis());
            scheduleBlockRemoval(player, block);
        }
    }

    private void scheduleBlockRemoval(@NotNull ParkourPlayer player, @NotNull Block block) {
        double blockLifetime = Config.CONFIG.getDouble("modes.speedrun.block-lifetime");
        double warningTime = Config.CONFIG.getDouble("modes.speedrun.warning-time");

        long removalTime = (long) (blockLifetime * 1000);
        long warningTimeMs = (long) (warningTime * 1000);

        Task.create(LoParkour.getPlugin())
            .delay((int) ((removalTime - warningTimeMs) / 50))
            .execute(() -> {
                if (block.getType() != Material.AIR) {
                    player.player.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    player.player.spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.01);
                }
            })
            .run();

        Task.create(LoParkour.getPlugin())
            .delay((int) (removalTime / 50))
            .execute(() -> {
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR);
                    blockTimestamps.remove(block);
                }
            })
            .run();
    }

    public void reset() {
        blockTimestamps.clear();
    }

    private static class SpeedrunGenerator extends ParkourGenerator {
        public SpeedrunGenerator(@NotNull Session session) {
            super(session);
        }

        @Override
        public Mode getMode() {
            return Modes.SPEEDRUN;
        }
    }
}
