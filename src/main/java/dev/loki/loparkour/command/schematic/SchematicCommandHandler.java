package dev.loki.loparkour.command.schematic;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.command.player.PlayerCommandHandler;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.schematic.convert.LpschemConverter;
import dev.loki.loparkour.schematic.core.ParkourSchematic;
import dev.loki.loparkour.schematic.create.SchematicCreator;
import dev.loki.loparkour.util.particle.ParticleUtil;
import dev.loki.loparkour.util.text.ColorUtil;
import dev.loki.loparkour.util.world.Locations;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin schematic tools: wand, create, convert, list, paste, reload.
 */
public class SchematicCommandHandler {

    private SchematicCommandHandler() {
    }

    public static final Map<Player, Location[]> SELECTIONS = new HashMap<>();
    public static final NamespacedKey WAND_KEY = new NamespacedKey(LoParkour.getPlugin(), "schematic_wand");

    private static ItemStack cachedWand;

    @NotNull
    public static ItemStack getWand() {
        return buildWand().clone();
    }

    public static boolean isWand(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(WAND_KEY, PersistentDataType.BYTE);
    }

    @NotNull
    public static Location[] getSelection(@NotNull Player player) {
        Location[] sel = SELECTIONS.get(player);
        if (sel == null) {
            sel = new Location[]{null, null};
            SELECTIONS.put(player, sel);
        }
        return sel;
    }

    public static void setPos(@NotNull Player player, @NotNull Location loc, int index) {
        Location[] updated = getSelection(player).clone();
        updated[index] = loc.clone();
        SELECTIONS.put(player, updated);
    }

    public static void clearWandCache() {
        cachedWand = null;
    }

    public static void sendHelp(@NotNull Player player) {
        send(player, "");
        send(player, Locales.getString(player, "schematic.help.header"));
        send(player, "");
        send(player, Locales.getString(player, "schematic.help.line_wand"));
        send(player, Locales.getString(player, "schematic.help.line_pos1"));
        send(player, Locales.getString(player, "schematic.help.line_pos2"));
        send(player, Locales.getString(player, "schematic.help.line_create"));
        send(player, Locales.getString(player, "schematic.help.line_create_named"));
        send(player, Locales.getString(player, "schematic.help.line_convert"));
        send(player, Locales.getString(player, "schematic.help.line_paste"));
        send(player, Locales.getString(player, "schematic.help.line_list"));
        send(player, Locales.getString(player, "schematic.help.line_reload"));
        send(player, Locales.getString(player, "schematic.help.line_folder"));
        send(player, Locales.getString(player, "schematic.help.line_docs"));
        send(player, "");
    }

    public static void handleSubcommand(String sub, CommandSender sender, Player player, PlayerCommandHandler cooldowns) {
        Location playerLoc = player.getLocation();
        Location[] sel = getSelection(player);

        switch (sub.toLowerCase()) {
            case "wand" -> {
                player.getInventory().addItem(getWand());
                send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.wand_given"));
            }
            case "pos1" -> setPosCommand(player, sel, playerLoc, 0);
            case "pos2" -> setPosCommand(player, sel, playerLoc, 1);
            case "list" -> {
                send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.list_header"));
                LoParkour.getSchematicManager().getAll().forEach((name, s) ->
                        send(player, Locales.getString(player, "schematic.list_entry")
                                .formatted(name, s.getDifficulty(), s.getFormat().name().toLowerCase())));
            }
            case "reload" -> {
                if (!cooldowns.cooldown(sender, "schematic-reload", 2500)) {
                    return;
                }
                Config.reload(false);
                LoParkour.getSchematicManager().reload();
                send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.reloaded"));
            }
            case "convert" -> handleConvert(sender, player, cooldowns);
            default -> sendHelp(player);
        }
    }

    public static void handleSubcommandWithName(String sub, String name, CommandSender sender, Player player) {
        switch (sub.toLowerCase()) {
            case "paste" -> handlePaste(name, sender, player);
            case "create" -> handleCreate(name, null, sender, player);
            default -> {
            }
        }
    }

    /** {@code schematic create <name> <difficulty>} */
    public static void handleCreateWithDifficulty(
            String name,
            String difficultyRaw,
            CommandSender sender,
            Player player,
            PlayerCommandHandler cooldowns
    ) {
        if (!cooldowns.cooldown(sender, "schematic-create", 2500)) {
            return;
        }
        handleCreate(name, difficultyRaw, sender, player);
    }

    /** {@code create <difficulty>} or {@code schematic create <difficulty>} */
    public static void handleCreateDifficultyOnly(
            String difficultyRaw,
            CommandSender sender,
            Player player,
            PlayerCommandHandler cooldowns
    ) {
        if (!cooldowns.cooldown(sender, "schematic-create", 2500)) {
            return;
        }
        handleCreate(null, difficultyRaw, sender, player);
    }

    private static void handleCreate(
            @Nullable String optionalName,
            @NotNull String difficultyRaw,
            CommandSender sender,
            Player player
    ) {
        Location[] sel = SELECTIONS.get(player);
        if (sel == null || sel[0] == null || sel[1] == null) {
            send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.selection_incomplete"));
            return;
        }

        double difficulty;
        try {
            difficulty = SchematicCreator.parseDifficulty(difficultyRaw);
        } catch (NumberFormatException e) {
            send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.invalid_difficulty"));
            return;
        } catch (IllegalArgumentException e) {
            send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.invalid_difficulty"));
            return;
        }

        try {
            SchematicCreator.CreateResult result = SchematicCreator.create(sel[0], sel[1], optionalName, difficulty);
            LoParkour.getSchematicManager().reload();
            send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.created")
                    .formatted(result.stem(), result.difficulty(), result.file().getName()));
        } catch (IllegalArgumentException e) {
            send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.invalid_name"));
        } catch (Exception e) {
            send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.create_failed").formatted(e.getMessage()));
        }
    }

    private static void handleConvert(CommandSender sender, Player player, PlayerCommandHandler cooldowns) {
        if (!cooldowns.cooldown(sender, "schematic-convert", 5000)) {
            return;
        }
        LpschemConverter.ConvertResult result = LpschemConverter.convertAll();
        if (result.converted() > 0) {
            LoParkour.getSchematicManager().reload();
        }
        send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.convert_done")
                .formatted(result.converted(), result.failed()));
        for (String line : result.messages()) {
            if (result.messages().size() <= 12) {
                send(player, "&#A0A0A0  " + line);
            }
        }
        if (result.messages().size() > 12) {
            send(player, Locales.getString(player, "schematic.convert_truncated").formatted(result.messages().size() - 12));
        }
    }

    private static void setPosCommand(Player player, Location[] existing, Location loc, int index) {
        setPos(player, loc, index);
        String label = index == 0 ? "1" : "2";
        send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.pos_set")
                .formatted(label, Locations.toString(loc, true)));

        Location[] updated = getSelection(player);
        if (updated[0] != null && updated[1] != null) {
            ParticleUtil.box(BoundingBox.of(updated[0], updated[1]), player.getWorld(), Particle.END_ROD, player, 0.2);
        }
    }

    private static void handlePaste(String name, CommandSender sender, Player player) {
        if (!isValidSchematicName(name)) {
            send(player, LoParkour.PREFIX + Locales.getString(player, "schematic.invalid_name"));
            return;
        }

        ParkourSchematic schematic = LoParkour.getSchematicManager().get(name);
        if (schematic == null) {
            send(sender, LoParkour.PREFIX + Locales.getString(sender, "schematic.not_found").formatted(name));
            return;
        }
        schematic.paste(player.getLocation(), player.getWorld());
        send(sender, LoParkour.PREFIX + Locales.getString(sender, "schematic.pasted").formatted(name));
    }

    static boolean isValidSchematicName(String name) {
        if (name == null || name.isEmpty() || name.length() > 64) {
            return false;
        }
        return name.matches("^[a-zA-Z0-9_-]+$");
    }

    @NotNull
    private static ItemStack buildWand() {
        if (cachedWand != null) {
            return cachedWand;
        }

        String locale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);
        if (locale == null || locale.isBlank()) {
            locale = "en";
        }

        ItemStack item = Locales.getItem(locale, "schematic.wand").build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        cachedWand = item;
        return item;
    }

    private static void send(CommandSender sender, String msg) {
        sender.sendMessage(ColorUtil.color(msg));
    }
}
