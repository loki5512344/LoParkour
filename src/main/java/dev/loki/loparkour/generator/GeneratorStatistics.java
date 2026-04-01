package dev.loki.loparkour.generator;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages generator statistics and time formatting.
 */
public class GeneratorStatistics {
    
    private final ParkourGenerator generator;
    
    public GeneratorStatistics(@NotNull ParkourGenerator generator) {
        this.generator = generator;
    }
    
    /**
     * Get formatted time string.
     */
    @NotNull
    public String getFormattedTime() {
        return getTime("mm:ss.SSS");
    }
    
    /**
     * Get detailed time string.
     */
    @NotNull
    public String getDetailedTime() {
        return getTime("HH:mm:ss.SSS");
    }
    
    /**
     * Register score in leaderboard.
     */
    public void registerScore(@NotNull String time, @NotNull String difficulty, int score) {
        Mode mode = generator.getMode();
        Leaderboard leaderboard = mode.getLeaderboard();
        
        for (ParkourPlayer player : generator.getPlayers()) {
            Score playerScore = new Score(
                player.player.getName(),
                time,
                difficulty,
                score
            );
            
            leaderboard.put(player.getUUID(), playerScore);
        }
    }
    
    /**
     * Send fall message to players.
     */
    public void sendFallMessage(int record) {
        for (ParkourPlayer player : generator.getPlayers()) {
            String message;
            
            if (generator.state.score > record) {
                // New record
                message = Locales.getString(player.player, "parkour.fall.new-record")
                    .replace("%score%", String.valueOf(generator.state.score))
                    .replace("%time%", getFormattedTime());
            } else {
                // Regular fall
                message = Locales.getString(player.player, "parkour.fall.regular")
                    .replace("%score%", String.valueOf(generator.state.score))
                    .replace("%record%", String.valueOf(record))
                    .replace("%time%", getFormattedTime());
            }
            
            player.player.sendMessage(message);
        }
    }
    
    @NotNull
    private String getTime(@NotNull String format) {
        if (generator.state.start == null) {
            return "00:00.000";
        }
        
        long elapsed = java.time.Duration.between(generator.state.start, java.time.Instant.now()).toMillis();
        Date date = new Date(elapsed);
        
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        
        return formatter.format(date);
    }
}