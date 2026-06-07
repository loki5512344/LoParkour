package dev.loki.loparkour.player.service;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.leaderboard.model.Score;
import dev.loki.loparkour.util.text.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.UUID;

/**
 * Manages scoreboard display for parkour users.
 *
 * @since 5.0.0
 */
public class ScoreboardManager {

    private static final String[] COLOR_CODES = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e", "f"
    };

    private final Player player;
    private final UUID playerUUID;
    private final Scoreboard board;
    private final String locale;

    public ScoreboardManager(Player player, UUID playerUUID, Scoreboard board, String locale) {
        this.player = player;
        this.playerUUID = playerUUID;
        this.board = board;
        this.locale = locale;
    }

    public void update(ParkourGenerator generator) {
        if (board == null || !generator.profile.get("showScoreboard").asBoolean()) {
            return;
        }

        Leaderboard leaderboard = generator.getMode().getLeaderboard();
        Score top = getTopScore(leaderboard);
        Score high = getHighScore(leaderboard);

        updateTitle(top, high, generator);
        updateLines(top, high, generator);
    }

    private Score getTopScore(Leaderboard leaderboard) {
        if (leaderboard == null) return createDefaultScore();
        Score score = leaderboard.getScoreAtRank(1);
        return score != null ? score : createDefaultScore();
    }

    private Score getHighScore(Leaderboard leaderboard) {
        if (leaderboard == null) return createDefaultScore();
        Score score = leaderboard.get(playerUUID);
        return score != null ? score : createDefaultScore();
    }

    private Score createDefaultScore() {
        return new Score("?", "?", "?", 0);
    }

    @SuppressWarnings("deprecation") // Scoreboard: legacy registerNewObjective(String,String,String)
    private void updateTitle(Score top, Score high, ParkourGenerator generator) {
        Objective obj = board.getObjective("lp_sidebar");
        if (obj == null) {
            obj = board.registerNewObjective("lp_sidebar", "dummy",
                    ColorUtil.color(replacePlaceholders(Locales.getString(locale, "scoreboard.title"), top, high, generator)));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.setDisplayName(ColorUtil.color(
                    replacePlaceholders(Locales.getString(locale, "scoreboard.title"), top, high, generator)));
        }
    }

    private void updateLines(Score top, Score high, ParkourGenerator generator) {
        Objective obj = board.getObjective("lp_sidebar");
        if (obj == null) return;

        List<String> lines = Locales.getStringList(locale, "scoreboard.lines").stream()
                .map(line -> replacePlaceholders(line, top, high, generator))
                .toList();

        for (int i = 0; i < lines.size(); i++) {
            String entry = createUniqueEntry(i);
            String teamName = "lp_line_" + i;

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.addEntry(entry);
            }

            team.setPrefix(ColorUtil.color(lines.get(i)));
            obj.getScore(entry).setScore(lines.size() - i);
        }
    }

    private String createUniqueEntry(int index) {
        return "§" + COLOR_CODES[index / COLOR_CODES.length % COLOR_CODES.length] +
               "§" + COLOR_CODES[index % COLOR_CODES.length];
    }

    private String replacePlaceholders(String text, Score top, Score high, ParkourGenerator generator) {
        String replaced = text
                .replace("%score%", Integer.toString(generator.state.score))
                .replace("%time%", generator.getFormattedTime())
                .replace("%difficulty%", Double.toString(generator.getDifficultyScore()))
                .replace("%top_score%", Integer.toString(top.score()))
                .replace("%top_player%", top.name())
                .replace("%top_time%", top.time())
                .replace("%high_score%", Integer.toString(high.score()))
                .replace("%high_score_time%", high.time());

        return applyPlaceholderAPI(replaced);
    }

    private String applyPlaceholderAPI(String text) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception e) {
                return text;
            }
        }
        return text;
    }
}
