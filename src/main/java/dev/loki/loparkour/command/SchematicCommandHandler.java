package dev.loki.loparkour.command;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import dev.loki.loparkour.schematic.lpschem.LPSchematicBuilder;
import dev.loki.loparkour.util.ColorUtil;
import dev.loki.loparkour.util.Locations;
import dev.loki.loparkour.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /LoParkour schematic sub-commands (wand, pos1, pos2, save, paste, list, reload).
 */
public class SchematicCommandHandler {

    public static final Map<Player, Location[]> selections = new HashMap<>();

    private static final ItemStack WAND = buildWand();

    private static ItemStack buildWand() {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("<red><bold>LPSchematic Wand"));
            meta.setLore(List.of(
                ColorUtil.color("<gray>Left click: first position"),
                ColorUtil.color("<gray>Right click: second position")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getWand() {
        return WAND.clone();
    }

    public static void sendHelp(Player player) {
        send(player, "");
        send(player, "<dark_red><bold>Schematics <reset><dark_gray>-----------");
        send(player, "<red>/LoParkour schematic wand   <dark_gray>- <gray>Get the wand");
        send(player, "<red>/LoParkour schematic pos1   <dark_gray>- <gray>Set position 1");
        send(player, "<red>/LoParkour schematic pos2   <dark_gray>- <gray>Set position 2");
        send(player, "<red>/LoParkour schematic save <n>  <dark_gray>- <gray>Save selection");
        send(player, "<red>/LoParkour schematic paste <n> <dark_gray>- <gray>Paste schematic");
        send(player, "<red>/LoParkour schematic list   <dark_gray>- <gray>List loaded schematics");
        send(player, "<red>/LoParkour schematic reload <dark_gray>- <gray>Reload schematics");
        send(player, "");
    }

    /** Called for 2-arg schematic commands (wand/pos1/pos2/list/reload) */
    public static void handleSubcommand(String sub, CommandSender sender, Player player, PlayerCommandHandler cooldowns) {
        Location playerLoc = player.getLocation();
        Location[] sel = selections.get(player);

        switch (sub.toLowerCase()) {
            case "wand" -> {
                player.getInventory().addItem(WAND.clone());
                send(player, "<dark_red><bold>Schematics <reset><gray>Wand given. Left/Right click to set positions.");
            }
            case "pos1" -> setPos(player, sel, playerLoc, 0);
            case "pos2" -> setPos(player, sel, playerLoc, 1);
            case "list" -> {
                send(player, "<dark_red><bold>Schematics <reset><gray>Loaded schematics:");
                LoParkour.getSchematicManager().getAllSchematics().forEach((name, s) ->
                    send(player, "<gray>- <red>" + name + " <dark_gray>(" + s.getMetadata().getDifficulty() + ")"));
            }
            case "reload" -> {
                if (!cooldowns.cooldown(sender, "schematic-reload", 2500)) return;
                LoParkour.getSchematicManager().reload();
                send(player, "<dark_red><bold>Schematics <reset><gray>Reloaded.");
            }
        }
    }

    /** Called for 3-arg schematic commands (save <n>, paste <n>) */
    public static void handleSubcommandWithName(String sub, String name, CommandSender sender,
                                                Player player, PlayerCommandHandler cooldowns) {
        switch (sub.toLowerCase()) {
            case "save" -> handleSave(name, sender, player, cooldowns);
            case "paste" -> handlePaste(name, sender, player);
        }
    }

    // ── internal ───────────────────────────────────────────────────────────────

    private static void setPos(Player player, Location[] existing, Location loc, int index) {
        String label = index == 0 ? "1" : "2";
        send(player, LoParkour.PREFIX + "Position " + label + " set to " + Locations.toString(loc, true));

        Location[] updated = existing != null ? existing.clone() : new Location[]{null, null};
        updated[index] = loc;
        selections.put(player, updated);

        if (updated[0] != null && updated[1] != null) {
            ParticleUtil.box(BoundingBox.of(updated[0], updated[1]), player.getWorld(), Particle.END_ROD, player, 0.2);
        }
    }

    private static void handleSave(String name, CommandSender sender, Player player, PlayerCommandHandler cooldowns) {
        if (!cooldowns.cooldown(sender, "schematic-save", 2500)) return;

        Location[] sel = selections.get(player);
        if (sel == null || sel[0] == null || sel[1] == null) {
            send(player, "<dark_red><bold>Schematics <reset><red>Selection incomplete. Set both positions first.");
            return;
        }

        try {
            int w = Math.abs(sel[1].getBlockX() - sel[0].getBlockX()) + 1;
            int h = Math.abs(sel[1].getBlockY() - sel[0].getBlockY()) + 1;
            int l = Math.abs(sel[1].getBlockZ() - sel[0].getBlockZ()) + 1;

            LPSchematic schematic = new LPSchematicBuilder(name, player.getName(), 0.5, w, h, l)
                    .fromWorld(player.getWorld(), sel[0], sel[1])
                    .build();

            LoParkour.getSchematicManager().saveSchematic(schematic);
            send(player, "<dark_red><bold>Schematics <reset><gray>Saved as <red>" + name + "<gray>!");
        } catch (Exception e) {
            send(player, "<dark_red><bold>Schematics <reset><red>Failed: " + e.getMessage());
        }
    }

    private static void handlePaste(String name, CommandSender sender, Player player) {
        LPSchematic s = LoParkour.getSchematicManager().getSchematic(name);
        if (s == null) {
            send(sender, LoParkour.PREFIX + "Schematic '" + name + "' not found.");
            return;
        }
        s.paste(player.getLocation(), player.getWorld());
        send(sender, LoParkour.PREFIX + "Pasted " + name + ".");
    }

    private static void send(CommandSender sender, String msg) {
        sender.sendMessage(ColorUtil.color(msg));
    }
}
