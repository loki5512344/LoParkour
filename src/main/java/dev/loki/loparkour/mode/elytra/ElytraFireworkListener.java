package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.player.core.ParkourPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.function.Function;

/**
 * Handles firework rocket use while gliding in elytra parkour.
 */
public final class ElytraFireworkListener implements Listener {

    @FunctionalInterface
    public interface FireworkUseHandler {
        /** @return true if the rocket was consumed */
        boolean onFirework(@NotNull ParkourPlayer parkourPlayer, @NotNull Player player);
    }

    private final Function<Player, ParkourPlayer> playerResolver;
    private final FireworkUseHandler handler;

    public ElytraFireworkListener(
            @NotNull Function<Player, ParkourPlayer> playerResolver,
            @NotNull FireworkUseHandler handler) {
        this.playerResolver = playerResolver;
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = playerResolver.apply(player);
        if (pp == null) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FIREWORK_ROCKET && handler.onFirework(pp, player)) {
            item.setAmount(item.getAmount() - 1);
            event.setCancelled(true);
        }
    }
}
