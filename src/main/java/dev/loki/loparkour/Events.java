package dev.loki.loparkour;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.loki.loparkour.util.ParticleData;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.world.World;
import org.bukkit.event.Listener;

import dev.loki.loparkour.util.ParticleUtil;
import dev.loki.loparkour.util.Locations;
import dev.loki.loparkour.util.ColorUtil;
import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Internal event handler
 */
@ApiStatus.Internal
public class Events implements Listener {

    @EventHandler
    public void chat(AsyncPlayerChatEvent event) {
        if (!Option.OPTIONS_ENABLED.get(ParkourOption.CHAT)) {
            return;
        }

        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);

        if (user == null) {
            return;
        }

        Session session = user.session;

        if (session.isMuted(user)) {
            return;
        }

        event.setCancelled(true);
        switch (user.chatType) {
            case LOBBY_ONLY -> session.getUsers().forEach(other -> other.sendTranslated("settings.chat.formats.lobby", player.getName(), event.getMessage()));
            case PLAYERS_ONLY -> session.getPlayers().forEach(other -> other.sendTranslated("settings.chat.formats.players", player.getName(), event.getMessage()));
            default -> event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
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
                .filter(world -> !world.equals(World.getWorld()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("No fallback world was found!"))
                .getSpawnLocation());
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        ParkourUser.unregister(user, true, false, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void command(PlayerCommandPreprocessEvent event) {
        if (!Config.CONFIG.getBoolean("focus-mode.enabled")) {
            return;
        }

        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        String command = event.getMessage().toLowerCase();
        if (Config.CONFIG.getStringList("focus-mode.whitelist").stream().anyMatch(c -> command.contains(c.toLowerCase()))) {
            return;
        }

        user.sendTranslated("other.no_do");
        event.setCancelled(true);
    }

    @EventHandler
    public void interactWand(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("LoParkour.admin") || item.getItemMeta() == null || !item.getItemMeta().getDisplayName().contains("LPSchematic Wand") || event.getClickedBlock() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Location location = event.getClickedBlock().getLocation();
        Location[] existingSelection = Command.selections.get(player);

        event.setCancelled(true);

        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                send(player, LoParkour.PREFIX + "Position 1 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    Command.selections.put(player, new Location[]{location, null});
                    return;
                }

                Command.selections.put(player, new Location[]{location, existingSelection[1]});

                ParticleUtil.box(BoundingBox.of(location, existingSelection[1]), player.getWorld(), Particle.END_ROD, player, 0.2);
            }
            case RIGHT_CLICK_BLOCK -> {
                send(player, LoParkour.PREFIX + "Position 2 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    Command.selections.put(player, new Location[]{null, location});
                    return;
                }

                Command.selections.put(player, new Location[]{existingSelection[0], location});

                ParticleUtil.box(BoundingBox.of(existingSelection[0], location), player.getWorld(), Particle.END_ROD, player, 0.2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);

        if (pp == null) {
            return;
        }

        boolean type = event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.DISPENSER || event.getClickedBlock().getType() == Material.DROPPER || event.getClickedBlock().getType() == Material.HOPPER);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && type && event.getHand() == EquipmentSlot.HAND) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;

        if (!action) {
            return;
        }

        Material held = getHeldItem(player).getType();

        Material play = Locales.getItem(player, "play.item").build().getType();
        Material community = Locales.getItem(player, "community.item").build().getType();
        Material settings = Locales.getItem(player, "settings.item").build().getType();
        Material lobby = Locales.getItem(player, "lobby.item").build().getType();
        Material quit = Locales.getItem(player, "other.quit").build().getType();

        event.setCancelled(true);

        if (held == play) {
            // TODO: Migrate to LoLib GUI
                // Menus.*.open(player);
                player.sendMessage("§cМеню временно недоступно во время миграции");
        } else if (held == community) {
            // TODO: Migrate to LoLib GUI
                // Menus.*.open(player);
                player.sendMessage("§cМеню временно недоступно во время миграции");
        } else if (held == settings) {
            // TODO: Migrate to LoLib GUI
                // Menus.*.open(player);
                player.sendMessage("§cМеню временно недоступно во время миграции");
        } else if (held == lobby) {
            // TODO: Migrate to LoLib GUI
                // Menus.*.open(player);
                player.sendMessage("§cМеню временно недоступно во время миграции");
        } else if (held == quit) {
            ParkourUser.leave(player);
        } else {
            if (!Config.CONFIG.getBoolean("options.disable-inventory-blocks")) {
                event.setCancelled(false);
            }
        }
    }

    private ItemStack getHeldItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.AIR ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }

    @EventHandler
    public void switchWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        org.bukkit.World parkour = World.getWorld();

        boolean isAdmin = Config.CONFIG.getBoolean("permissions.enabled") ? ParkourOption.ADMIN.mayPerform(player) : player.isOp();

        if (player.getWorld() == parkour && user == null && !isAdmin && player.getTicksLived() > 20) {
            Bukkit.getWorlds().stream()
                    .filter(world -> !world.equals(parkour))
                    .findAny()
                    .ifPresent(world -> PaperLib.teleportAsync(player, world.getSpawnLocation()));
            return;
        }

        if (event.getFrom() == parkour && user != null && Duration.between(user.joined, Instant.now()).toMillis() > 100) {
            ParkourUser.unregister(user, true, false, false);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getInventory().getType() == InventoryType.CRAFTING) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void spectate(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }

        handleRestriction(event.getPlayer(), event);
    }

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void mount(EntityMountEvent event) {
//        if (!(event.getEntity() instanceof Player player)) {
//            return;
//        }
//
//        handleRestriction(player, event);
//    }

    private void handleRestriction(Player player, Cancellable event) {
        if (!ParkourUser.isUser(player)) {
            return;
        }

        event.setCancelled(true);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.color(message));
    }
}
