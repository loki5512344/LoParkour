package dev.loki.loparkour.mode;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Coop Mode - Shared score between multiple players
 */
public class CoopMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "coop";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return Locales.getItem(locale, "modes.coop");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage("§cJoining is currently disabled.");
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof CoopMode) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new CoopGenerator(session), null, null, player);
    }

    private static class CoopGenerator extends ParkourGenerator {
        private final Map<UUID, Integer> playerContributions = new HashMap<>();
        private int sharedScore = 0;

        public CoopGenerator(@NotNull Session session) {
            super(session);
            
            // Initialize contributions
            for (ParkourPlayer pp : session.getPlayers()) {
                playerContributions.put(pp.getUUID(), 0);
            }
        }

        @Override
        protected void score() {
            super.score();
            sharedScore++;
            
            // Track individual contributions
            if (!getPlayers().isEmpty()) {
                UUID scorer = player.getUUID();
                playerContributions.put(scorer, playerContributions.getOrDefault(scorer, 0) + 1);
            }
            
            // Milestone celebrations
            if (sharedScore % 50 == 0) {
                for (ParkourPlayer pp : getPlayers()) {
                    pp.player.sendTitle("§6" + sharedScore + " Points!", "§eTeam effort!", 5, 30, 10);
                    pp.player.playSound(pp.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            
            // Display shared score and contributions in ActionBar
            for (ParkourPlayer pp : getPlayers()) {
                int myContribution = playerContributions.getOrDefault(pp.getUUID(), 0);
                int percentage = sharedScore > 0 ? (myContribution * 100 / sharedScore) : 0;
                
                String message = "§aTeam: §e" + sharedScore + " §7| §bYou: §e" + myContribution + " §7(" + percentage + "%)";
                pp.player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
                );
            }
        }

        @Override
        protected void fall() {
            // Show final stats before reset
            for (ParkourPlayer pp : getPlayers()) {
                int contribution = playerContributions.getOrDefault(pp.getUUID(), 0);
                pp.sendTranslated("modes.coop.stats", 
                    Integer.toString(sharedScore), 
                    Integer.toString(contribution));
            }
            
            super.fall();
            
            // Reset coop state
            sharedScore = 0;
            playerContributions.clear();
            for (ParkourPlayer pp : getPlayers()) {
                playerContributions.put(pp.getUUID(), 0);
            }
        }

        @Override
        public Mode getMode() {
            return Modes.COOP;
        }
    }
}
