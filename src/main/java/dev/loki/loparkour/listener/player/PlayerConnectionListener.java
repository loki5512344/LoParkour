package dev.loki.loparkour.listener.player;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.loki.loparkour.world.core.World;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Handles join / quit / world-change events.
 */
public class PlayerConnectionListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            Modes.DEFAULT.create(player);
            return;
        }

        if (!player.getWorld().equals(World.getWorld())) {
            return;
        }

        org.bukkit.World fallback = Bukkit.getWorld(Config.CONFIG.getString("world.fall-back"));
        if (fallback != null) {
            PaperLib.teleportAsync(player, fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            return;
        }

        PaperLib.teleportAsync(player, Bukkit.getWorlds().stream()
                .filter(w -> !w.equals(World.getWorld()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("No fallback world found!"))
                .getSpawnLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());
        if (user == null) {
            return;
        }
        ParkourUser.unregister(user, true, false, true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        org.bukkit.World parkour = World.getWorld();

        boolean isAdmin = Config.CONFIG.getBoolean("permissions.enabled")
                ? ParkourOption.ADMIN.mayPerform(player)
                : player.isOp();

        if (player.getWorld() == parkour && user == null && !isAdmin && player.getTicksLived() > 20) {
            Bukkit.getWorlds().stream()
                    .filter(w -> !w.equals(parkour))
                    .findAny()
                    .ifPresent(w -> PaperLib.teleportAsync(player, w.getSpawnLocation()));
            return;
        }

        if (event.getFrom() == parkour && user != null
                && Duration.between(user.joined, Instant.now()).toMillis() > 100) {
            ParkourUser.unregister(user, true, false, false);
        }
    }
}
