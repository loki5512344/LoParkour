package dev.loki.loparkour.hook.holo;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.leaderboard.model.Score;
import dev.loki.loparkour.mode.base.Mode;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;

public class HoloHook {

    /**
     * Initializes this hook.
     */
    public static void init() {
        try {
            Class.forName("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI");
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().warning("##");
            LoParkour.getPlugin().getLogger().warning("## LoParkour only supports Holographic Displays v3.0.0 or higher!");
            LoParkour.getPlugin().getLogger().warning("## This hook will now be disabled.");
            LoParkour.getPlugin().getLogger().warning("##");
            return;
        }

        HolographicDisplaysAPI.get(LoParkour.getPlugin()).registerGlobalPlaceholder("ip_leaderboard", 100, argument -> {

            if (argument == null) {
                return "?";
            }

            // {ip_leaderboard: default, score, #1}
            String[] split = argument.replace(" ", "").split(",");

            Mode mode = Registry.getMode(split[0].toLowerCase());

            if (mode == null) {
                return "?";
            }

            Leaderboard leaderboard = mode.getLeaderboard();

            if (leaderboard == null) {
                return "?";
            }

            String type = split[1].toLowerCase();
            String rank = split[2].replace("#", "");

            Score score;
            try {
                score = leaderboard.getScoreAtRank(Integer.parseInt(rank));
            } catch (NumberFormatException e) {
                return "?";
            }

            if (score == null) {
                return "?";
            }

            return switch (type) {
                case "score" -> Integer.toString(score.score());
                case "name" -> score.name();
                case "time" -> score.time();
                case "difficulty" -> score.difficulty();
                case "difficulty_string" -> parseDifficulty(score.difficulty());
                default -> "?";
            };
        });
    }

    private static String parseDifficulty(String string) {
        if (string.contains("?")) {
            return "?";
        }

        try {
            double difficulty = Double.parseDouble(string);
            return parseDifficultyValue(difficulty);
        } catch (NumberFormatException e) {
            return "?";
        }
    }

    private static String parseDifficultyValue(double difficulty) {
        if (difficulty <= 0.25) {
            return "easy";
        } else if (difficulty <= 0.5) {
            return "medium";
        } else if (difficulty <= 0.75) {
            return "hard";
        } else if (difficulty <= 1) {
            return "very hard";
        }
        return "?";
    }
}
