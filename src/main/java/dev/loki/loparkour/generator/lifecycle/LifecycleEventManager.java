package dev.loki.loparkour.generator.lifecycle;

import dev.loki.loparkour.api.event.ParkourFallEvent;
import dev.loki.loparkour.api.event.ParkourScoreEvent;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.reward.Reward;
import dev.loki.loparkour.reward.Rewards;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Manages lifecycle events for parkour generator.
 */
public class LifecycleEventManager {
    
    private final ParkourGenerator generator;
    
    public LifecycleEventManager(@NotNull ParkourGenerator generator) {
        this.generator = generator;
    }
    
    /**
     * Handle score event and check rewards.
     */
    public void handleScore() {
        // Fire score event for each player
        for (ParkourPlayer player : generator.getPlayers()) {
            ParkourScoreEvent scoreEvent = new ParkourScoreEvent(player);
            Bukkit.getPluginManager().callEvent(scoreEvent);
        }
        
        // Check for rewards
        checkRewards();
    }
    
    /**
     * Handle fall event.
     */
    public void handleFall() {
        // Fire fall event for each player
        for (ParkourPlayer player : generator.getPlayers()) {
            ParkourFallEvent fallEvent = new ParkourFallEvent(player);
            Bukkit.getPluginManager().callEvent(fallEvent);
        }
        
        // Process fall logic
        processFall();
    }
    
    private void checkRewards() {
        if (!Rewards.REWARDS_ENABLED) {
            return;
        }
        
        Mode mode = generator.getMode();
        
        for (ParkourPlayer player : generator.getPlayers()) {
            int score = generator.state.score;
            
            // Score-based rewards
            if (Rewards.SCORE_REWARDS.containsKey(score)) {
                for (Reward reward : Rewards.SCORE_REWARDS.get(score)) {
                    reward.execute(player, mode);
                }
            }
            
            // Interval rewards (every N blocks)
            for (Map.Entry<Integer, List<Reward>> entry : Rewards.INTERVAL_REWARDS.entrySet()) {
                if (score > 0 && score % entry.getKey() == 0) {
                    for (Reward reward : entry.getValue()) {
                        reward.execute(player, mode);
                    }
                }
            }
            
            // One-time milestone rewards
            if (Rewards.ONE_TIME_REWARDS.containsKey(score)) {
                for (Reward reward : Rewards.ONE_TIME_REWARDS.get(score)) {
                    reward.execute(player, mode);
                }
            }
        }
    }
    
    private void processFall() {
        // Record statistics
        String time = generator.getFormattedTime();
        String difficulty = String.valueOf(generator.getDifficultyScore());
        
        generator.registerScore(time, difficulty, generator.state.score);
        
        // Reset generator state
        generator.reset(true);
    }
}