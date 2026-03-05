package dev.loki.loparkour.generator;

import dev.loki.loparkour.api.event.ParkourScoreEvent;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.reward.Rewards;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages scoring and rewards (SRP - Single Responsibility)
 */
public class ScoreManager {
    
    private final ParkourPlayer player;
    private final Mode mode;
    private final Profile profile;
    
    private int score = 0;
    private int totalScore = 0;
    private Instant start;
    
    public ScoreManager(ParkourPlayer player, Mode mode, Profile profile) {
        this.player = player;
        this.mode = mode;
        this.profile = profile;
    }
    
    public void addScore(int points) {
        for (int i = 0; i < points; i++) {
            score++;
            totalScore++;
            checkRewards();
            new ParkourScoreEvent(player).call();
        }
        
        if (start == null) {
            start = Instant.now();
        }
    }
    
    private void checkRewards() {
        if (!Rewards.REWARDS_ENABLED || score == 0) {
            return;
        }

        if (Rewards.SCORE_REWARDS.containsKey(score)) {
            Rewards.SCORE_REWARDS.get(score).forEach(s -> s.execute(player, mode));
        }

        int intervalScore = Config.CONFIG.getBoolean("scoring.rewards-use-total-score") ? totalScore : score;
        for (int interval : Rewards.INTERVAL_REWARDS.keySet()) {
            if (intervalScore % interval != 0) continue;
            Rewards.INTERVAL_REWARDS.get(interval).forEach(s -> s.execute(player, mode));
        }

        if (Rewards.ONE_TIME_REWARDS.containsKey(score) && 
            !player.collectedRewards.contains(Integer.toString(score))) {
            Rewards.ONE_TIME_REWARDS.get(score).forEach(s -> s.execute(player, mode));
            player.collectedRewards.add(Integer.toString(score));
        }
    }
    
    public void reset() {
        score = 0;
        start = null;
    }
    
    public void registerScore(List<ParkourPlayer> players) {
        Leaderboard leaderboard = mode.getLeaderboard();
        if (leaderboard == null) return;

        String time = getDetailedTime();
        String difficulty = Double.toString(getDifficultyScore()).substring(0, Math.min(3, Double.toString(getDifficultyScore()).length()));
        
        players.forEach(p -> leaderboard.put(p.getUUID(), new Score(p.getName(), time, difficulty, score)));
    }
    
    public String getFormattedTime() {
        return getTime(Config.CONFIG.getString("options.time.score-format"));
    }
    
    public String getDetailedTime() {
        return getTime("mm:ss:SSS");
    }
    
    private String getTime(String format) {
        var timeMs = Instant.now().minusMillis(start != null ? start.toEpochMilli() : Instant.now().toEpochMilli());

        try {
            return DateTimeFormatter.ofPattern(format)
                    .withZone(ZoneOffset.UTC)
                    .format(timeMs);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
    
    private double getDifficultyScore() {
        double score = 0;
        if (profile.get("useSpecialBlocks").asBoolean()) score += 0.5;
        
        double schematicDiff = profile.get("schematicDifficulty").asDouble();
        if (schematicDiff > 0) {
            if (schematicDiff <= 0.25) score += 0.2;
            else if (schematicDiff <= 0.5) score += 0.3;
            else if (schematicDiff <= 0.75) score += 0.4;
            else score += 0.5;
        }
        return score;
    }
    
    public int getScore() {
        return score;
    }
    
    public int getTotalScore() {
        return totalScore;
    }
}
