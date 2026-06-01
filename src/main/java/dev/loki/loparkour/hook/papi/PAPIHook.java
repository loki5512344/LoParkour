package dev.loki.loparkour.hook.papi;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.leaderboard.model.Score;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.player.spectator.ParkourSpectator;
import dev.loki.loparkour.player.core.ParkourUser;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PAPIHook extends PlaceholderExpansion {

    private static final Pattern INFINITE_REGEX = Pattern.compile("(.+)_(\\d+)");

    @Override
    public @NotNull String getIdentifier() {
        return "witp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "loki";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull String getVersion() {
        return LoParkour.getPlugin().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        // placeholders that don't require a player
        switch (params) {
            case "version", "ver" -> {
                return LoParkour.getPlugin().getDescription().getVersion();
            }
            case "leader", "record_player" -> {
                Score score = Modes.DEFAULT.getLeaderboard().getScoreAtRank(1);
                return score != null ? score.name() : "?";
            }
            case "leader_score", "record_score", "record" -> {
                Score score = Modes.DEFAULT.getLeaderboard().getScoreAtRank(1);
                return score != null ? Integer.toString(score.score()) : "?";
            }
        }

        if (params.contains("player_rank_")) {
            return getInfiniteScore(params.replace("player_rank_", ""), Score::name);
        } else if (params.contains("score_rank_")) {
            return getInfiniteScore(params.replace("score_rank_", ""), Score::score);
        } else if (params.contains("time_rank_")) {
            return getInfiniteScore(params.replace("time_rank_", ""), Score::time);
        } else if (params.contains("difficulty_rank_")) {
            return getInfiniteScore(params.replace("difficulty_rank_", ""), Score::difficulty);
        } else if (params.contains("difficulty_string_rank_")) {
            return getInfiniteScore(params.replace("difficulty_string_rank_", ""),
                    score -> {
                        String diff = score.difficulty();
                        if ("?".equals(diff)) {
                            return parseDifficulty(2.0);
                        }
                        try {
                            return parseDifficulty(Double.parseDouble(diff));
                        } catch (NumberFormatException e) {
                            return parseDifficulty(2.0);
                        }
                    });
        }

        // placeholders that require player
        if (player == null) {
            return "player doesn't exist";
        }

        switch (params) {
            case "rank" -> {
                return Integer.toString(Modes.DEFAULT.getLeaderboard().getRank(player.getUniqueId()));
            }
            case "highscore", "high_score" -> {
                return Integer.toString(Modes.DEFAULT.getLeaderboard().get(player.getUniqueId()).score());
            }
            case "high_score_time" -> {
                return Modes.DEFAULT.getLeaderboard().get(player.getUniqueId()).time();
            }
        }

        ParkourUser user = ParkourUser.getUser(player);
        ParkourPlayer pp = null;
        if (user instanceof ParkourPlayer) {
            pp = (ParkourPlayer) user;
        } else if (user instanceof ParkourSpectator) {
            pp = ((ParkourSpectator) user).closest;
        }

        if (pp != null && pp.session.generator != null) {
            ParkourGenerator generator = pp.session.generator;
            switch (params) {
                case "score", "current_score" -> {
                    return Integer.toString(generator.state.score);
                }
                case "time", "current_time" -> {
                    return generator.getFormattedTime();
                }
                case "blocklead", "lead" -> {
                    return Integer.toString(pp.blockLead);
                }
                case "style" -> {
                    return pp.style;
                }
                case "time_pref", "time_preference" -> {
                    return Integer.toString(pp.selectedTime);
                }
                case "scoreboard" -> {
                    return pp.showScoreboard.toString();
                }
                case "difficulty" -> {
                    return Double.toString(pp.schematicDifficulty);
                }
                case "difficulty_string" -> {
                    return parseDifficulty(pp.schematicDifficulty);
                }
                default -> {
                    if (params.contains("score_until_")) {
                        String replaced = params.replace("score_until_", "");
                        try {
                            int interval = Integer.parseInt(replaced);
                            if (interval > 0) {
                                return Integer.toString(interval - (generator.state.score % interval));
                            } else {
                                return "0";
                            }
                        } catch (NumberFormatException e) {
                            return "0";
                        }
                    }
                }
            }
        }

        return null;
    }

    private String parseDifficulty(double difficulty) {
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

    private String getInfiniteScore(String rankData, Function<Score, ?> f) {
        int rank;
        Leaderboard leaderboard = null;
        Matcher matcher = INFINITE_REGEX.matcher(rankData);

        try {
            // use mode-specific format
            // x_mode_rank
            if (matcher.matches()) {
                String name = matcher.group(1);
                rank = Integer.parseInt(matcher.group(2));

                Mode mode = Registry.getMode(name);
                if (mode != null) {
                    leaderboard = mode.getLeaderboard();
                }
                // use generic format
                // x_rank
            } else {
                rank = Integer.parseInt(rankData);
            }
        } catch (NumberFormatException e) {
            return "?";
        }

        if (leaderboard == null) {
            leaderboard = Modes.DEFAULT.getLeaderboard();
        }

        if (rank > 0) {
            Score score = leaderboard.getScoreAtRank(rank);

            if (score == null) {
                return "?";
            } else {
                return String.valueOf(f.apply(score));
            }
        } else {
            return "?";
        }
    }
}
