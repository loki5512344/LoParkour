package dev.loki.loparkour.generator;

import dev.lolib.scheduler.Scheduler;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourFallEvent;
import dev.loki.loparkour.api.event.ParkourScoreEvent;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.reward.Rewards;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Iterator;

/**
 * Handles parkour lifecycle: tick, fall, reset, cleanup.
 */
public class GeneratorLifecycle {

    private final ParkourGenerator generator;
    private dev.lolib.scheduler.ScheduledTask cleanupTask;

    public GeneratorLifecycle(ParkourGenerator generator) {
        this.generator = generator;
    }

    public void startTick() {
        generator.task = Scheduler.get(LoParkour.getPlugin()).runTimer(this::tick, 0, 1);
        cleanupTask = Scheduler.get(LoParkour.getPlugin()).runTimer(this::cleanupDistantBlocks, 0, Option.CLEANUP_INTERVAL);
    }

    public void tick() {
        if (generator.state.stopped) {
            generator.task.cancel();
            if (cleanupTask != null) cleanupTask.cancel();
            return;
        }

        generator.getPlayers().forEach(p -> {
            updateVisualTime(p, p.selectedTime);
            p.updateScoreboard(generator);
            p.player.setSaturation(20);
        });
        generator.getSpectators().forEach(ParkourSpectator::update);

        if (generator.player.getLocation().getWorld() != generator.state.lastStandingPlayerLocation.getWorld()) return;
        if (generator.player.getLocation().subtract(generator.state.lastStandingPlayerLocation).getY() < -10) {
            fall();
            return;
        }

        Block below = blockBelow();
        if (below == null) return;

        handleSchematicEndBlock(below);
        if (!generator.state.history.contains(below)) return;

        int idx = generator.state.history.indexOf(below);
        int delta = idx - generator.state.lastPositionIndexPlayer;
        if (delta <= 0) return;

        generator.state.lastStandingPlayerLocation = generator.player.getLocation();

        int lead = generator.profile.get("blockLead").asInt();
        if (generator.state.history.size() - idx <= lead) {
            generator.generate(lead - (generator.state.history.size() - idx));
        }
        generator.state.lastPositionIndexPlayer = idx;

        removeTrailBlocks(idx);
        cleanupDistantBlocks();
        generator.placer.deleteSchematic();

        int pts = Config.CONFIG.getBoolean("scoring.all-points") ? delta : 1;
        for (int i = 0; i < pts; i++) score();

        if (generator.state.start == null) generator.state.start = Instant.now();
    }

    private void handleSchematicEndBlock(Block below) {
        if (generator.state.schematicBlocks.contains(below) 
            && below.getType() == Material.RED_WOOL 
            && !generator.state.deleteSchematic) {
            
            for (int i = 0; i < generator.profile.get("schematicDifficulty").asDouble() * 15; i++) {
                score();
            }
            generator.state.waitForSchematicCompletion = false;
            generator.state.schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
            generator.generate(generator.profile.get("blockLead").asInt());
            generator.state.deleteSchematic = true;
        }
    }

    private @Nullable Block blockBelow() {
        Location loc = generator.player.getLocation().subtract(0, 1, 0);
        Block b = loc.getBlock();
        if (b.getType() == Material.AIR) {
            if (loc.subtract(0, 0.5, 0).getBlock().getType() == Material.AIR) return null;
            b = loc.getBlock();
        }
        return b;
    }

    private void removeTrailBlocks(int idx) {
        for (int i = idx - ParkourGenerator.BLOCK_TRAIL - 1; i >= idx - 4 * ParkourGenerator.BLOCK_TRAIL; i--) {
            if (i > 0) generator.state.history.get(i).setType(Material.AIR);
        }
    }

    private void updateVisualTime(ParkourPlayer p, int selectedTime) {
        int t = 18000 + selectedTime;
        if (t >= 24000) t -= 24000;
        p.player.setPlayerTime(t, false);
    }

    protected void score() {
        generator.state.score++;
        generator.state.totalScore++;
        checkRewards();
        new ParkourScoreEvent(generator.player).call();
    }

    private void checkRewards() {
        if (!Rewards.REWARDS_ENABLED || generator.state.score == 0) return;

        if (Rewards.SCORE_REWARDS.containsKey(generator.state.score)) {
            Rewards.SCORE_REWARDS.get(generator.state.score).forEach(r -> r.execute(generator.player, generator.getMode()));
        }

        int intervalScore = Config.CONFIG.getBoolean("scoring.rewards-use-total-score") 
            ? generator.state.totalScore 
            : generator.state.score;
            
        for (int interval : Rewards.INTERVAL_REWARDS.keySet()) {
            if (intervalScore % interval == 0) {
                Rewards.INTERVAL_REWARDS.get(interval).forEach(r -> r.execute(generator.player, generator.getMode()));
            }
        }

        String key = Integer.toString(generator.state.score);
        if (Rewards.ONE_TIME_REWARDS.containsKey(generator.state.score) 
            && !generator.player.collectedRewards.contains(key)) {
            Rewards.ONE_TIME_REWARDS.get(generator.state.score).forEach(r -> r.execute(generator.player, generator.getMode()));
            generator.player.collectedRewards.add(key);
        }
    }

    protected void fall() {
        new ParkourFallEvent(generator.player).call();
        generator.reset(true);
    }

    protected void cleanupDistantBlocks() {
        if (generator.state.history.size() < Option.BLOCK_CLEANUP_DISTANCE * 2) return;
        
        Location loc = generator.player.getLocation();
        int removed = 0;
        Iterator<Block> it = generator.state.history.iterator();
        
        while (it.hasNext()) {
            Block b = it.next();
            if (b.getLocation().distance(loc) > Option.BLOCK_CLEANUP_DISTANCE) {
                b.setType(Material.AIR, false);
                it.remove();
                removed++;
            } else break;
        }
        
        if (removed > 0) {
            generator.state.lastPositionIndexPlayer = Math.max(0, generator.state.lastPositionIndexPlayer - removed);
        }
    }
}
