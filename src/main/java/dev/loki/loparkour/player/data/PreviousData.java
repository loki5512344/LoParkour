package dev.loki.loparkour.player.data;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.reward.Reward;
import io.papermc.lib.PaperLib;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class for storing previous data of players
 */
public class PreviousData {

    private final InventoryData inventoryData;

    private final int hunger;
    private final boolean flying;
    private final boolean allowFlight;
    private final GameMode gamemode;
    private final Location location;
    private final Collection<PotionEffect> effects;

    /**
     * List of all {@link Reward} to execute on leave.
     */
    public List<Reward> onLeave = new ArrayList<>();

    public PreviousData(@NotNull Player player) {
        gamemode = player.getGameMode();
        location = player.getLocation();
        hunger = player.getFoodLevel();
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        effects = player.getActivePotionEffects();

        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }

        if (Config.CONFIG.getBoolean("options.inventory-handling")) {
            inventoryData = new InventoryData(player);
            inventoryData.save(Config.CONFIG.getBoolean("options.inventory-saving"));
        } else {
            inventoryData = null;
        }
    }

    public void apply(Player player, boolean urgent) {
        var to = Config.CONFIG.getBoolean("bungeecord.go-back-enabled") ? Option.GO_BACK_LOC : location;

        if (!urgent)
            PaperLib.teleportAsync(player, to).thenRun(() -> apply(player));
        else {
            player.teleport(to);

            apply(player);
        }
    }

    private void apply(Player player) {
        try {
            player.setFoodLevel(hunger);
            player.setGameMode(gamemode);
            player.setAllowFlight(allowFlight);
            player.setFlying(flying);

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
            }

            player.resetPlayerTime();
            player.resetPlayerWeather();
            player.setVelocity(new Vector(0, 0, 0));
            player.setFallDistance(0f);
        } catch (Exception ex) { // not the best way to do this... too bad!
            LoParkour.getPlugin().getLogger().severe("Error while recovering stats of %s".formatted(player.getName()) + " - " + ex.getMessage());
        }

        if (inventoryData != null) {
            inventoryData.apply();
        }
    }
}
