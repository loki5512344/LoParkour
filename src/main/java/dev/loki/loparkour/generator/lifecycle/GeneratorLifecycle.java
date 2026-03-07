package dev.loki.loparkour.generator.lifecycle;

import dev.lolib.scheduler.Scheduler;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourFallEvent;
import dev.loki.loparkour.api.event.ParkourScoreEvent;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.ghost.GhostData;
import dev.loki.loparkour.ghost.GhostManager;
import dev.loki.loparkour.ghost.GhostRecorder;
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
    private GhostRecorder ghostRecorder;
    private GhostManager ghostManager;

    public GeneratorLifecycle(ParkourGenerator generator) {
        this.generator = generator;
        
        if (Config.CONFIG.getBoolean("ghost-mode.enabled")) {
            this.ghostRecorder = new GhostRecorder();
            this.ghostManager = new GhostManager();
            this.ghostManager.loadGhosts(generator.getMode().getName());
        }
    }

    public void startTick() {
        generator.task = Scheduler.get(LoParkour.getPlugin()).runTimer(this::tick, 0, 1);
        cleanupTask = Scheduler.get(LoParkour.getPlugin()).runTimer(this::cleanupDistantBlocks, 0, Option.CLEANUP_INTERVAL);
        
        // Start ghost recording if enabled
        if (ghostRecorder != null && ghostManager != null) {
            ghostRecorder.startRecording(generator.player.getLocation());
            
            // Spawn top ghosts
            if (generator.state.playerSpawn != null) {
                ghostManager.spawnGhosts(
                    generator.getMode().getName(),
                    generator.state.playerSpawn,
                    generator.player.player.getWorld()
                );
            }
        }
    }

    public void tick() {
        if (generator.state.stopped) {
            generator.task.cancel();
            if (cleanupTask != null) cleanupTask.cancel();
            if (ghostManager != null) ghostManager.stopAllGhosts();
            return;
        }

        generator.getPlayers().forEach(p -> {
            updateVisualTime(p, p.selectedTime);
            p.updateScoreboard(generator);
            p.player.setSaturation(20);
            
            // Record ghost frame
            if (ghostRecorder != null && ghostRecorder.isRecording()) {
                ghostRecorder.recordFrame(p.getLocation());
            }
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
        
        // Clean up schematic blocks that are behind the player
        if (!generator.state.schematicBlocks.isEmpty()) {
            int currentIdx = generator.state.history.indexOf(below);
            if (currentIdx > 0) {
                generator.state.schematicBlocks.removeIf(b -> {
                    int blockIdx = generator.state.history.indexOf(b);
                    if (blockIdx >= 0 && blockIdx < currentIdx - 5) {
                        b.setType(Material.AIR);
                        return true;
                    }
                    return false;
                });
            }
        }
        
        // Reset schematic wait if player is far from schematic blocks
        if (generator.state.waitForSchematicCompletion && !generator.state.schematicBlocks.isEmpty()) {
            boolean nearSchematic = generator.state.schematicBlocks.stream()
                .anyMatch(b -> b.getLocation().distance(generator.player.getLocation()) < 15);
            if (!nearSchematic) {
                generator.state.waitForSchematicCompletion = false;
                // Force delete schematic blocks
                generator.state.schematicBlocks.forEach(b -> b.setType(Material.AIR));
                generator.state.schematicBlocks.clear();
                generator.state.deleteSchematic = false;
                generator.state.schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
            }
        }
        
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

    public void score() {
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

    public void fall() {
        new ParkourFallEvent(generator.player).call();
        
        // Save ghost if score is good enough
        if (ghostRecorder != null && ghostManager != null && ghostRecorder.isRecording()) {
            String modeName = generator.getMode().getName();
            int score = generator.state.score;
            
            if (ghostManager.shouldRecordGhost(modeName, score)) {
                GhostData ghostData = ghostRecorder.stopRecording(
                    generator.player.getName(),
                    score
                );
                ghostManager.saveGhost(modeName, ghostData);
                generator.player.sendTranslated("ghost.saved", Integer.toString(score));
            }
        }
        
        if (ghostManager != null) {
            ghostManager.stopAllGhosts();
        }
        
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
