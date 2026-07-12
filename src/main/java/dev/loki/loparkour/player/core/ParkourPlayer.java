package dev.loki.loparkour.player.core;

import com.google.gson.annotations.Expose;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.generator.core.model.Profile;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.mode.base.MultiMode;
import dev.loki.loparkour.player.data.PreviousData;
import dev.loki.loparkour.player.service.ParkourHotbar;
import dev.loki.loparkour.player.service.PlayerSettingsManager;
import dev.loki.loparkour.session.core.Session;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.world.core.Divider;
import dev.lolib.scheduler.Scheduler;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode.
 *
 * @author loki
 */
public class ParkourPlayer extends ParkourUser {

    public static final Map<String, PlayerSettingsManager.OptionContainer> PLAYER_COLUMNS = PlayerSettingsManager.getColumnMappings();

    // Track scored blocks to prevent duplicate scoring (String key = "x,y,z" to avoid Bukkit Block reference equality)
    private final Set<String> scoredBlocks = new HashSet<>();

    public @Expose Double schematicDifficulty;
    public @Expose Integer blockLead;
    public @Expose Boolean particles;
    public @Expose Boolean sound;
    public @Expose Boolean useSpecialBlocks;
    public @Expose Boolean showFallMessage;
    public @Expose Boolean showScoreboard;
    public @Expose Integer selectedTime;
    public @Expose String style;
    public @Expose String locale;
    public @Expose List<String> collectedRewards;
    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player, Session)} instead
     */
    public ParkourPlayer(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, session, previousData);

        // generic player settings
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvisible(false);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    /**
     * @param player The player.
     * @return True when this player is a {@link ParkourPlayer}, false if not.
     */
    public static boolean isPlayer(@Nullable Player player) {
        return player != null && getPlayers().stream().anyMatch(other -> other.player == player);
    }

    /**
     * @param player The player.
     * @return player as a {@link ParkourPlayer}, null if not found.
     */
    public static @Nullable ParkourPlayer getPlayer(@NotNull Player player) {
        // Optimized: search directly in sessions without creating intermediate list
        return Divider.sections.keySet().stream()
                .flatMap(session -> session.getPlayers().stream())
                .filter(other -> other.getUUID().equals(player.getUniqueId()))
                .findAny()
                .orElse(null);
    }

    /**
     * @return List with all players.
     */
    public static List<ParkourPlayer> getPlayers() {
        return Divider.sections.keySet().stream()
                .flatMap(session -> session.getPlayers().stream())
                .toList();
    }

    @Override
    public void unregister() {
        if (session.generator != null &&
                session.generator.getMode() != null &&
                session.generator.getMode() instanceof MultiMode mode) {
            mode.leave(player, session);
        }

        session.removePlayers(this);
        
        // Clear scored blocks to prevent memory leak
        scoredBlocks.clear();

        save(LoParkour.getPlugin().isEnabled());
    }

    /**
     * Clear scored blocks set, typically on fall/respawn so same coordinates can be re-scored.
     */
    public void clearScoredBlocks() {
        scoredBlocks.clear();
    }

    private static String blockKey(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }

    /**
     * Check if player has already scored on this block.
     */
    public boolean hasScored(Block block) {
        return scoredBlocks.contains(blockKey(block));
    }

    /**
     * Mark block as scored to prevent duplicate scoring.
     */
    public void markScored(Block block) {
        scoredBlocks.add(blockKey(block));
    }

    /**
     * Sets the user's settings. If an item is not included, the setting gets reset.
     *
     * @param settings The settings map.
     */
    public void setSettings(@NotNull Map<String, Object> settings) {
        PlayerSettingsManager.applySettings(this, settings);
    }

    /**
     * Forces this player's generator to match the settings of this player.
     */
    public void updateGeneratorSettings(ParkourGenerator generator) {
        Profile profile = generator.profile;

        profile.set("schematicDifficulty", schematicDifficulty.toString())
                .set("blockLead", blockLead.toString())
                .set("particles", particles.toString())
                .set("sound", sound.toString())
                .set("useSpecialBlocks", useSpecialBlocks.toString())
                .set("showFallMessage", showFallMessage.toString())
                .set("showScoreboard", showScoreboard.toString())
                .set("selectedTime", selectedTime.toString())
                .set("style", style);

        generator.profileManager.overrideProfile();
        generator.profileManager.calculateChances();
    }

    /**
     * Saves the player's data to their file
     */
    public void save(boolean async) {
        Runnable write = () -> Storage.writePlayer(this);

        if (async) {
            Scheduler.get(LoParkour.getPlugin()).runAsync(write);
        } else {
            write.run();
        }
    }

    public void setup(Location to) {
        if (to != null) {
            teleport(to);
        }

        player.setGameMode(GameMode.ADVENTURE);

        if (Config.CONFIG.getBoolean("options.inventory-handling")) {
            Scheduler.get(LoParkour.getPlugin()).runLater(() -> ParkourHotbar.apply(this), 5);
        } else {
            sendTranslated("other.customize");
        }
    }

    /**
     * Updates hotbar items with current language
     */
    public void updateHotbar() {
        ParkourHotbar.apply(this);
    }

    public record OptionContainer(ParkourOption option, BiConsumer<ParkourPlayer, String> consumer) {
    }
}
