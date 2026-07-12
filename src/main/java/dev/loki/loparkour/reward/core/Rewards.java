package dev.loki.loparkour.reward.core;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that reads the rewards-v2.yml file and puts them in the variables listed below.
 */
public class Rewards {

    private Rewards() {
    }

    public static boolean REWARDS_ENABLED;

    /**
     * A map with all Score-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, List<Reward>> SCORE_REWARDS = new HashMap<>();

    /**
     * A map with all Interval-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, List<Reward>> INTERVAL_REWARDS = new HashMap<>();

    /**
     * A map with all One time-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, List<Reward>> ONE_TIME_REWARDS = new HashMap<>();

    /**
     * Reads the rewards from the rewards-v2.yml file
     */
    public static void init() {
        // init options
        REWARDS_ENABLED = Config.REWARDS.getBoolean("enabled");

        if (!REWARDS_ENABLED) {
            return;
        }

        SCORE_REWARDS = parseScores("score-rewards");
        INTERVAL_REWARDS = parseScores("interval-rewards");
        ONE_TIME_REWARDS = parseScores("one-time-rewards");
    }
    
    /**
     * Clear all reward maps to prevent memory leaks on reload
     */
    public static void clear() {
        SCORE_REWARDS.clear();
        INTERVAL_REWARDS.clear();
        ONE_TIME_REWARDS.clear();
    }

    private static Map<Integer, List<Reward>> parseScores(String path) {
        Map<Integer, List<Reward>> rewardMap = new HashMap<>();

        for (String score : Config.REWARDS.getChildren(path)) {

            // read commands for this score
            List<Reward> rewardStrings = Config.REWARDS.getStringList("%s.%s".formatted(path, score)).stream()
                    .map(Reward::new)
                    .toList();

            try {
                int value = Integer.parseInt(score);

                if (value < 1) {
                    LoParkour.getPlugin().getLogger().severe(
                            "Error while trying to read rewards - check the rewards file for incorrect numbers - "
                                    + value + " is not a valid score");
                    continue;
                }

                rewardMap.put(value, rewardStrings);
            } catch (NumberFormatException ex) {
                LoParkour.getPlugin().getLogger().severe(
                        "Error while trying to read rewards - check the rewards file for incorrect numbers - "
                                + ex.getMessage());
            }
        }

        return rewardMap;
    }
}
