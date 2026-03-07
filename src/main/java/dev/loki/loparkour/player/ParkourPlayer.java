package dev.loki.loparkour.player;

import java.util.ArrayList;

import dev.lolib.scheduler.Scheduler;

import com.google.gson.annotations.Expose;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.generator.Profile;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.MultiMode;
import dev.loki.loparkour.player.data.PreviousData;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.world.Divider;

import dev.lolib.scheduler.Scheduler;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode.
 *
 * @author loki
 */
public class ParkourPlayer extends ParkourUser {

    public static final Map<String, OptionContainer> PLAYER_COLUMNS = PlayerSettingsManager.getColumnMappings();

    public @Expose Double schematicDifficulty;
    public @Expose Integer blockLead;
    public @Expose Boolean particles;
    public @Expose Boolean sound;
    public @Expose Boolean useSpecialBlocks;
    public @Expose Boolean showFallMessage;
    public @Expose Boolean showScoreboard;
    public @Expose Integer selectedTime;
    public @Expose String style;
    public @Expose String _locale;
    public @Expose List<String> collectedRewards;
    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player, Session)} instead
     */
    public ParkourPlayer(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, session, previousData);

        this._locale = locale;

        // generic player settings
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvisible(false);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private static boolean parseBoolean(String string) {
        return string == null || string.equals("1") || string.equals("true");
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
        return getPlayers().stream()
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

        save(LoParkour.getPlugin().isEnabled());
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

        generator.overrideProfile();
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
            Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
                player.getInventory().clear();

                List<dev.loki.loparkour.util.Item> items = new ArrayList<>();

                if (ParkourOption.PLAY.mayPerform(player)) items.add(Locales.getItem(locale, "play.item"));
                if (ParkourOption.COMMUNITY.mayPerform(player)) items.add(Locales.getItem(locale, "community.item"));
                if (ParkourOption.SETTINGS.mayPerform(player)) items.add(Locales.getItem(locale, "settings.item"));
                if (ParkourOption.LOBBY.mayPerform(player)) items.add(Locales.getItem(locale, "lobby.item"));

                if (ParkourOption.QUIT.mayPerform(player)) items.add(Locales.getItem(locale, "other.quit"));

                List<Integer> slots = getEvenlyDistributedSlots(items.size());
                for (int idx = 0; idx < items.size(); idx++) {
                    player.getInventory().setItem(slots.get(idx), items.get(idx).build());
                }
            }, 5);
        } else {
            sendTranslated("other.customize");
        }
    }

    public record OptionContainer(ParkourOption option, BiConsumer<ParkourPlayer, String> consumer) {
    }

    private List<Integer> getEvenlyDistributedSlots(int count) {
        List<Integer> slots = new ArrayList<>();
        int[] positions = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(count, positions.length); i++) {
            slots.add(positions[i]);
        }
        return slots;
    }
}
