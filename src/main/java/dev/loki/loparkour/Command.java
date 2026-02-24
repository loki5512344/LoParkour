package dev.loki.loparkour;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.mode.MultiMode;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.player.data.InventoryData;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import dev.loki.loparkour.schematic.lpschem.LPSchematicBuilder;
import dev.loki.loparkour.schematic.lpschem.SchematicConverter;
import dev.loki.loparkour.session.Session;
import dev.efnilite.vilib.command.ViCommand;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.schematic.Schematic;
import dev.efnilite.vilib.schematic.Schematics;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Strings;
import org.bukkit.*;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("deprecation")
public class Command extends ViCommand {

    public static final HashMap<Player, Location[]> selections = new HashMap<>();

    private static final ItemStack WAND = new Item(Material.GOLDEN_AXE, "<red><bold>Schematic Wand")
            .lore("<gray>Left click: first position", "<gray>Right click: second position")
            .build();

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        switch (args.length) {
            case 0 -> handle0Args(sender, player);
            case 1 -> handle1Args(args[0], sender, player);
            case 2 -> handle2Args(args[0], args[1], sender, player);
            case 3 -> handle3Args(args[0], args[1], args[2], sender, player);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> {
                if (ParkourOption.JOIN.mayPerform(sender)) {
                    completions.add("join");
                    completions.add("leave");
                }
                if (ParkourOption.MAIN.mayPerform(sender)) {
                    completions.add("menu");
                }
                if (ParkourOption.PLAY.mayPerform(sender)) {
                    completions.add("play");
                }
                if (ParkourOption.LEADERBOARDS.mayPerform(sender)) {
                    completions.add("leaderboard");
                }
                if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("schematic");
                    completions.add("reload");
                    completions.add("forcejoin");
                    completions.add("forceleave");
                    completions.add("reset");
                    completions.add("recoverinventory");
                }
                return completions(args[0], completions);
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("reset") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("everyone");
                    for (ParkourPlayer pp : ParkourPlayer.getPlayers()) {
                        completions.add(pp.getName());
                    }
                } else if (args[0].equalsIgnoreCase("join") && sender.hasPermission(ParkourOption.JOIN.permission)) {
                    for (ParkourPlayer pp : ParkourPlayer.getPlayers()) {
                        completions.add(pp.getName());
                    }
                } else if (args[0].equalsIgnoreCase("schematic") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.addAll(Arrays.asList("wand", "pos1", "pos2", "save", "paste"));
                } else if (args[0].equalsIgnoreCase("forcejoin") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("nearest");
                    completions.add("everyone");

                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                } else if (args[0].equalsIgnoreCase("forceleave") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("everyone");

                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                } else if (args[0].equalsIgnoreCase("recoverinventory") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                }
                return completions(args[1], completions);
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    private void handle0Args(@NotNull CommandSender sender, @Nullable Player player) {
        if (player != null && ParkourOption.MAIN.mayPerform(player)) {
            Menus.MAIN.open(player);
            return;
        }
        sendHelpMessages(sender);
    }

    private void sendHelpMessages(CommandSender sender) {
        send(sender, "");
        send(sender, "<dark_gray><strikethrough>---------------<reset> %s <dark_gray><strikethrough>---------------<reset>".formatted(LoParkour.NAME));
        send(sender, "");
        send(sender, "<gray>/parkour <dark_gray>- Main command");
        if (sender.hasPermission(ParkourOption.JOIN.permission)) {
            send(sender, "<gray>/parkour join [mode/player] <dark_gray>- Join the default mode or specify one.");
            send(sender, "<gray>/parkour leave <dark_gray>- Leave the game on this server");
        }
        if (sender.hasPermission(ParkourOption.MAIN.permission)) {
            send(sender, "<gray>/parkour menu <dark_gray>- Open the menu");
        }
        if (sender.hasPermission(ParkourOption.PLAY.permission)) {
            send(sender, "<gray>/parkour play <dark_gray>- Mode selection menu");
        }
        if (sender.hasPermission(ParkourOption.LEADERBOARDS.permission)) {
            send(sender, "<gray>/parkour leaderboard [type]<dark_gray>- Open the leaderboard of a mode");
        }
        if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, "<gray>/LoParkour schematic <dark_gray>- Create a schematic");
            send(sender, "<gray>/LoParkour reload <dark_gray>- Reloads the messages-v3.yml file");
            send(sender, "<gray>/LoParkour reset <everyone/player> <dark_gray>- Resets all high scores. <red>This can't be recovered!");
            send(sender, "<gray>/LoParkour forcejoin <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to join");
            send(sender, "<gray>/LoParkour forceleave <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to leave");
            send(sender, "<gray>/LoParkour recoverinventory <player> <dark_gray>- Recover a player's saved inventory. <red>Useful for recovering data after server crashes or errors when leaving.");
        }
        send(sender, "");
    }

    private void handle1Args(@NotNull String arg, @NotNull CommandSender sender, @Nullable Player player) {
        switch (arg.toLowerCase()) {
            case "help" -> sendHelpMessages(sender);
            case "reload" -> {
                if (!cooldown(sender, "reload", 2500)) {
                    return;
                }
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Config.reload(false);

                send(sender, "%sReloaded config files.".formatted(LoParkour.PREFIX));
            }
        }

        if (player == null) {
            return;
        }

        switch (arg.toLowerCase()) {
            case "join" -> {
                if (!cooldown(sender, "join", 2500)) {
                    return;
                }

                if (!ParkourOption.JOIN.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    return;
                }

                Modes.DEFAULT.create(player);
            }
            case "play" -> {
                if (ParkourOption.PLAY.mayPerform(player)) {
                    Menus.PLAY.open(player);
                }
            }
            case "leave" -> {
                if (!cooldown(sender, "leave", 2500)) {
                    return;
                }
                ParkourUser.leave(player);
            }
            case "menu", "main" -> {
                if (!ParkourOption.MAIN.mayPerform(player)) {
                    Menus.MAIN.open(player);
                }
            }
            case "leaderboard" -> player.performCommand("LoParkour leaderboard invalid");
            case "schematic" -> {
                if (!player.hasPermission(ParkourOption.ADMIN.permission)) { // default players shouldn't have access even if perms are disabled
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                send(player, "");
                send(player, "<red>/LoParkour schematic wand <dark_gray>- <gray>Get the schematic wand");
                send(player, "<red>/LoParkour schematic pos1 <dark_gray>- <gray>Set the first position of your selection");
                send(player, "<red>/LoParkour schematic pos2 <dark_gray>- <gray>Set the second position of your selection");
                send(player, "<red>/LoParkour schematic save <name> <dark_gray>- <gray>Save selection to .lpschem format");
                send(player, "<red>/LoParkour schematic paste <name> <dark_gray>- <gray>Paste a .lpschem schematic");
                send(player, "<red>/LoParkour schematic list <dark_gray>- <gray>List all loaded schematics");
                send(player, "<red>/LoParkour schematic reload <dark_gray>- <gray>Reload all schematics");
                send(player, "<red>/LoParkour schematic convert <dark_gray>- <gray>Convert old schematics to .lpschem");
                send(player, "");
                send(player, "<dark_gray><underlined>Have any questions or need help? Join the Discord.");
            }
        }
    }

    private void handle2Args(@NotNull String arg1, @NotNull String arg2, @NotNull CommandSender sender, @Nullable Player player) {
        switch (arg1.toLowerCase()) {
            case "forcejoin" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    Bukkit.getOnlinePlayers().forEach(other -> Modes.DEFAULT.create(other));
                    send(sender, LoParkour.PREFIX + "Successfully force joined everyone!");
                    return;
                }

                if (arg2.equalsIgnoreCase("nearest")) {
                    Player closest = null;
                    double distance = Double.MAX_VALUE;

                    // if player is found get location from player
                    // if no player is found, get location from command block
                    // if no command block is found, return null
                    Location from = sender instanceof Player ? ((Player) sender).getLocation() : (sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getLocation() : null);

                    if (from == null || from.getWorld() == null) {
                        return;
                    }

                    // get the closest player
                    for (Player p : from.getWorld().getPlayers()) {
                        double d = p.getLocation().distance(from);

                        if (d < distance) {
                            distance = d;
                            closest = p;
                        }
                    }

                    // no closest player found
                    if (closest == null) {
                        return;
                    }

                    send(sender, LoParkour.PREFIX + "Successfully force joined " + closest.getName() + "!");
                    Modes.DEFAULT.create(closest);
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, LoParkour.PREFIX + "That player isn't online!");
                    return;
                }

                Modes.DEFAULT.create(other);
            }
            case "forceleave" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    ParkourPlayer.getPlayers().forEach(ParkourUser::leave);
                    send(sender, LoParkour.PREFIX + "Successfully force kicked everyone!");
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, LoParkour.PREFIX + "That player isn't online!");
                    return;
                }

                ParkourUser user = ParkourUser.getUser(other);
                if (user == null) {
                    send(sender, LoParkour.PREFIX + "That player isn't currently playing!");
                    return;
                }

                ParkourUser.leave(user);
            }
            case "reset" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission) || !cooldown(sender, "reset", 2500)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    for (Mode mode : Registry.getModes()) {
                        Leaderboard leaderboard = mode.getLeaderboard();

                        if (leaderboard == null) {
                            continue;
                        }

                        leaderboard.resetAll();
                        leaderboard.write(true);
                    }

                    send(sender, LoParkour.PREFIX + "Successfully reset all high scores in memory and the files.");
                    return;
                }
                String name = null;
                UUID uuid = null;

                // Check online players
                Player online = Bukkit.getPlayerExact(arg2);
                if (online != null) {
                    name = online.getName();
                    uuid = online.getUniqueId();
                }

                // Check uuid
                if (arg2.contains("-")) {
                    uuid = UUID.fromString(arg2);
                }

                // Check offline player
                if (uuid == null) {
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(arg2);
                    name = offline.getName();
                    uuid = offline.getUniqueId();
                }

                UUID finalUuid = uuid;
                String finalName = name;

                for (Mode mode : Registry.getModes()) {
                    Leaderboard leaderboard = mode.getLeaderboard();

                    if (leaderboard == null) {
                        continue;
                    }

                    leaderboard.remove(finalUuid);
                    leaderboard.write(true);
                }

                send(sender, LoParkour.PREFIX + "Successfully reset the high score of " + finalName + " in memory and the files.");
            }
            case "recoverinventory" -> {
                if (!cooldown(sender, "recoverinventory", 2500) || !sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, LoParkour.PREFIX + "That player isn't online!");
                    return;
                }

                new InventoryData(other).load(result -> {
                    if (result != null) {
                        send(sender, "%sSuccessfully recovered the inventory of %s from their file".formatted(LoParkour.PREFIX, other.getName()));
                        send(sender, "%sGiving %s their items now...".formatted(LoParkour.PREFIX, other.getName()));
                    } else {
                        send(sender, "%s<red>There was an error recovering the inventory of %s from their file".formatted(LoParkour.PREFIX, other.getName()));
                        send(sender, "%s%s has no saved inventory or there was an error. Check the console.".formatted(LoParkour.PREFIX, other.getName()));
                    }
                });
            }
        }

        if (player == null) {
            return;
        }

        switch (arg1) {
            case "join" -> {
                if (!cooldown(sender, "join", 2500) || !ParkourOption.JOIN.mayPerform(player)) {
                    return;
                }

                Mode mode = Registry.getMode(arg2);

                if (mode != null) {
                    mode.create(player);
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);

                if (other == null) {
                    send(sender, "%sUnknown player! Try typing the name again.".formatted(LoParkour.PREFIX)); // could not find, so go to default
                    return;
                }

                ParkourPlayer parkourPlayer = ParkourPlayer.getPlayer(other);

                if (parkourPlayer == null) {
                    send(sender, "%sUnknown player! Try typing the name again.".formatted(LoParkour.PREFIX)); // could not find, so go to default
                    return;
                }

                ParkourUser user = ParkourUser.getUser(player);
                Session session = parkourPlayer.session;
                if (user != null && user.session == session) { // already in same session
                    return;
                }

                if (session.isAcceptingPlayers()) {
                    ((MultiMode) session.generator.getMode()).join(player, session);
                } else {
                    Modes.SPECTATOR.create(player, session);
                }
            }
            case "leaderboard" -> {
                if (!ParkourOption.LEADERBOARDS.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Mode mode = Registry.getMode(arg2.toLowerCase());

                // if found gamemode is null, return to default
                if (mode == null) {
                    Menus.LEADERBOARDS.open(player);
                } else {
                    Menus.SINGLE_LEADERBOARD.open(player, mode, Leaderboard.Sort.SCORE);
                }
            }
            case "schematic" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Location playerLocation = player.getLocation();
                Location[] existingSelection = selections.get(player);

                switch (arg2.toLowerCase()) {
                    case "wand" -> {
                        player.getInventory().addItem(WAND);

                        send(player, "<dark_gray>----------- <dark_red><bold>Schematics <reset><dark_gray>-----------");
                        send(player, "<gray><red>Left click<gray> -> set first position | <red>Right click<gray> -> set second position");
                        send(player, "<gray>If you can't place a block and need to set a position mid-air, use <dark_gray>/LoParkour schematic pos1/pos2 <gray>instead.");
                    }
                    case "pos1" -> {
                        send(player, "%sPosition 1 was set to %s".formatted(LoParkour.PREFIX, Locations.toString(playerLocation, true)));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{playerLocation, null});
                            return;
                        }

                        selections.put(player, new Location[]{playerLocation, existingSelection[1]});

                        Particles.box(BoundingBox.of(playerLocation, existingSelection[1]), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "pos2" -> {
                        send(player, "%sPosition 2 was set to %s".formatted(LoParkour.PREFIX, Locations.toString(playerLocation, true)));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{null, playerLocation});
                            return;
                        }

                        selections.put(player, new Location[]{existingSelection[0], playerLocation});

                        Particles.box(BoundingBox.of(existingSelection[0], playerLocation), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "save" -> {
                        if (!cooldown(sender, "LoParkour save schematic", 2500)) {
                            return;
                        }

                        if (existingSelection == null || existingSelection[0] == null || existingSelection[1] == null) {
                            send(player, "<dark_red><bold>Schematics <reset><gray>Your schematic isn't complete yet. Make sure you've set the first and second position.");
                            return;
                        }

                        String code = UUID.randomUUID().toString().split("-")[0];

                        send(player, ("<dark_red><bold>Schematics <reset><gray>Your schematic is being saved. It will use code <red>'%s'<gray>. " + "You can change the code to whatever you like. " + "Don't forget to add this schematic to <dark_gray>schematics.yml<gray>.").formatted(code));

                        Schematic.save(LoParkour.getInFolder("schematics/parkour-%s".formatted(code)), existingSelection[0], existingSelection[1], LoParkour.getPlugin());
                    }
                    case "list" -> {
                        send(player, "<dark_red><bold>Schematics <reset><gray>Loaded schematics:");
                        LoParkour.getSchematicManager().getAllSchematics().forEach((name, schem) -> {
                            send(player, "<gray>- <red>" + name + " <dark_gray>(" + schem.getMetadata().getDifficulty() + ")");
                        });
                    }
                    case "reload" -> {
                        if (!cooldown(sender, "LoParkour reload schematics", 2500)) {
                            return;
                        }
                        LoParkour.getSchematicManager().reload();
                        send(player, "<dark_red><bold>Schematics <reset><gray>Reloaded all schematics.");
                    }
                    case "convert" -> {
                        if (!cooldown(sender, "LoParkour convert schematics", 5000)) {
                            return;
                        }
                        send(player, "<dark_red><bold>Schematics <reset><gray>Converting old schematics to .lpschem format...");
                        SchematicConverter.convertAll();
                        send(player, "<dark_red><bold>Schematics <reset><gray>Conversion complete! Check console for details.");
                    }
                }
            }
        }
    }

    private void handle3Args(@NotNull String arg1, @NotNull String arg2, @NotNull String arg3, @NotNull CommandSender sender, @Nullable Player player) {
        if (player == null) {
            return;
        }

        if (arg1.equalsIgnoreCase("schematic")) {
            if (!player.hasPermission(ParkourOption.ADMIN.permission)) {
                return;
            }

            switch (arg2.toLowerCase()) {
                case "save" -> {
                    if (!cooldown(sender, "LoParkour save schematic", 2500)) {
                        return;
                    }

                    Location[] selection = selections.get(player);
                    if (selection == null || selection[0] == null || selection[1] == null) {
                        send(player, "<dark_red><bold>Schematics <reset><gray>Your schematic isn't complete yet. Make sure you've set the first and second position.");
                        return;
                    }

                    try {
                        int width = Math.abs(selection[1].getBlockX() - selection[0].getBlockX()) + 1;
                        int height = Math.abs(selection[1].getBlockY() - selection[0].getBlockY()) + 1;
                        int length = Math.abs(selection[1].getBlockZ() - selection[0].getBlockZ()) + 1;

                        LPSchematic schematic = new LPSchematicBuilder(arg3, player.getName(), 0.5, width, height, length)
                                .fromWorld(player.getWorld(), selection[0], selection[1])
                                .build();

                        LoParkour.getSchematicManager().saveSchematic(schematic);
                        send(player, "<dark_red><bold>Schematics <reset><gray>Saved schematic as <red>" + arg3 + "<gray>!");
                    } catch (Exception e) {
                        send(player, "<dark_red><bold>Schematics <reset><gray>Failed to save schematic: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                case "paste" -> {
                    LPSchematic lpSchematic = LoParkour.getSchematicManager().getSchematic(arg3);
                    if (lpSchematic != null) {
                        lpSchematic.paste(player.getLocation(), player.getWorld());
                        send(sender, "%sPasted .lpschem schematic %s".formatted(LoParkour.PREFIX, arg3));
                        return;
                    }

                    Schematic oldSchematic = Schematics.getSchematic(LoParkour.getPlugin(), arg3);
                    if (oldSchematic == null) {
                        send(sender, "%sCouldn't find %s".formatted(LoParkour.PREFIX, arg3));
                        return;
                    }

                    oldSchematic.paste(player.getLocation());
                    send(sender, "%sPasted old schematic %s".formatted(LoParkour.PREFIX, arg3));
                }
            }
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Strings.colour(message));
    }
}
