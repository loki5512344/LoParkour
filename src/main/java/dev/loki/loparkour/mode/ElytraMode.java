package dev.loki.loparkour.mode;

import dev.loki.loparkour.util.Item;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Elytra Parkour Mode - Flying parkour with rings and boost
 */
public class ElytraMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "elytra";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return Locales.getItem(locale, "modes.elytra");
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
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof ElytraMode) {
            return;
        }

        player.closeInventory();

        // Create session with elytra setup
        Session.create(session -> {
            ParkourGenerator generator = new ParkourGenerator(session);
            
            // Give elytra and fireworks to all players in session
            for (ParkourPlayer parkourPlayer : session.getPlayers()) {
                Player p = parkourPlayer.player;
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
                
                int fireworks = Config.CONFIG.getInt("modes.elytra.starting-fireworks");
                p.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, fireworks));
            }
            
            return generator;
        }, null, null, player);
    }
}
