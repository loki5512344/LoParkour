package dev.loki.loparkour.generator.lifecycle;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Manages visual updates for players during parkour lifecycle.
 */
public class LifecycleVisualUpdater {
    
    private final ParkourGenerator generator;
    
    public LifecycleVisualUpdater(@NotNull ParkourGenerator generator) {
        this.generator = generator;
    }
    
    /**
     * Update visuals for all players.
     */
    public void updateAllPlayers() {
        for (ParkourPlayer player : generator.getPlayers()) {
            updatePlayerVisuals(player);
        }
    }
    
    /**
     * Update time displays for all players.
     */
    public void updateTimeDisplays() {
        for (ParkourPlayer player : generator.getPlayers()) {
            updatePlayerTimeDisplay(player);
        }
    }
    
    private void updatePlayerVisuals(@NotNull ParkourPlayer player) {
        // Update scoreboard if enabled
        if (player.showScoreboard != null && player.showScoreboard) {
            player.updateScoreboard(generator);
        }
        
        // Update action bar with time and score
        updatePlayerTimeDisplay(player);
    }
    
    @SuppressWarnings("unused") // reserved for action bar
    private void updatePlayerTimeDisplay(@NotNull ParkourPlayer player) {
        // Placeholder for action bar (Paper adventure); scoreboard already shows time/score.
    }
}
