package dev.loki.loparkour.listener;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.command.SchematicCommandHandler;
import dev.loki.loparkour.util.ColorUtil;
import dev.loki.loparkour.util.Locations;
import dev.loki.loparkour.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

/**
 * Handles LPSchematic wand clicks (left/right click on blocks).
 */
public class SchematicWandListener implements Listener {

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("LoParkour.admin")) return;
        if (item.getItemMeta() == null || !item.getItemMeta().getDisplayName().contains("LPSchematic Wand")) return;
        if (event.getClickedBlock() == null || event.getHand() != EquipmentSlot.HAND) return;

        Location loc = event.getClickedBlock().getLocation();
        Location[] sel = SchematicCommandHandler.selections.get(player);
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Location[] updated = new Location[]{loc, sel != null ? sel[1] : null};
            SchematicCommandHandler.selections.put(player, updated);
            send(player, LoParkour.PREFIX + "Position 1 set to " + Locations.toString(loc, true));
            if (updated[1] != null)
                ParticleUtil.box(BoundingBox.of(loc, updated[1]), player.getWorld(), Particle.END_ROD, player, 0.2);

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Location[] updated = new Location[]{sel != null ? sel[0] : null, loc};
            SchematicCommandHandler.selections.put(player, updated);
            send(player, LoParkour.PREFIX + "Position 2 set to " + Locations.toString(loc, true));
            if (updated[0] != null)
                ParticleUtil.box(BoundingBox.of(updated[0], loc), player.getWorld(), Particle.END_ROD, player, 0.2);
        }
    }

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(ColorUtil.color(msg));
    }
}
