package dev.loki.loparkour.listener.schematic;

import dev.loki.loparkour.command.schematic.SchematicCommandHandler;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.util.particle.ParticleUtil;
import dev.loki.loparkour.util.text.ColorUtil;
import dev.loki.loparkour.util.world.Locations;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

/**
 * Wand left/right click sets schematic selection corners.
 */
public class SchematicWandListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!SchematicCommandHandler.isWand(event.getItem())) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        event.setCancelled(true);
        var player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        int index = action == Action.LEFT_CLICK_BLOCK ? 0 : 1;

        SchematicCommandHandler.setPos(player, loc, index);
        String label = index == 0 ? "1" : "2";
        player.sendMessage(ColorUtil.color(
                Locales.getString(player, "schematic.pos_set").formatted(label, Locations.toString(loc, true))));

        Location[] sel = SchematicCommandHandler.getSelection(player);
        if (sel[0] != null && sel[1] != null) {
            ParticleUtil.box(
                    org.bukkit.util.BoundingBox.of(sel[0], sel[1]),
                    player.getWorld(),
                    Particle.END_ROD,
                    player,
                    0.2
            );
        }
    }
}
