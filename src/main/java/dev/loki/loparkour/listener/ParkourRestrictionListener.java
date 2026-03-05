package dev.loki.loparkour.listener;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.session.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Handles hotbar item interactions, chat routing, focus mode, and
 * general restrictions (drop/place/break/damage) for parkour players.
 */
public class ParkourRestrictionListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!Option.OPTIONS_ENABLED.get(ParkourOption.CHAT)) return;

        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        if (user == null) return;

        Session session = user.session;
        if (session.isMuted(user)) return;

        event.setCancelled(true);
        switch (user.chatType) {
            case LOBBY_ONLY   -> session.getUsers().forEach(u -> u.sendTranslated("settings.chat.formats.lobby", player.getName(), event.getMessage()));
            case PLAYERS_ONLY -> session.getPlayers().forEach(u -> u.sendTranslated("settings.chat.formats.players", player.getName(), event.getMessage()));
            default           -> event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!Config.CONFIG.getBoolean("focus-mode.enabled")) return;
        ParkourUser user = ParkourUser.getUser(event.getPlayer());
        if (user == null) return;

        String cmd = event.getMessage().toLowerCase();
        if (Config.CONFIG.getStringList("focus-mode.whitelist").stream().anyMatch(cmd::contains)) return;

        user.sendTranslated("other.no_do");
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp == null) return;

        // Block container right-click
        boolean isContainer = event.getClickedBlock() != null && switch (event.getClickedBlock().getType()) {
            case DISPENSER, DROPPER, HOPPER -> true;
            default -> false;
        };
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                && isContainer && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        boolean isRightClick = (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
                && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;
        if (!isRightClick) return;

        org.bukkit.Material held = heldItem(player).getType();
        event.setCancelled(true);

        if      (held == Locales.getItem(player, "play.item").build().getType())      Menus.PLAY.open(player);
        else if (held == Locales.getItem(player, "community.item").build().getType()) Menus.COMMUNITY.open(player);
        else if (held == Locales.getItem(player, "settings.item").build().getType())  Menus.SETTINGS.open(player);
        else if (held == Locales.getItem(player, "lobby.item").build().getType())     Menus.LOBBY.open(player);
        else if (held == Locales.getItem(player, "other.quit").build().getType())     ParkourUser.leave(player);
        else if (!Config.CONFIG.getBoolean("options.disable-inventory-blocks"))       event.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() == InventoryType.CHEST) return;   // LoLib GUI
        if (event.getInventory().getType() == InventoryType.CRAFTING) return; // own inv
        restrict(player, event);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event)   { restrict(event.getPlayer(), event); }
    @EventHandler
    public void onPlace(BlockPlaceEvent event)      { restrict(event.getPlayer(), event); }
    @EventHandler
    public void onBreak(BlockBreakEvent event)      { restrict(event.getPlayer(), event); }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) restrict(p, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectate(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE)
            restrict(event.getPlayer(), event);
    }

    private void restrict(Player player, Cancellable event) {
        if (ParkourUser.isUser(player)) event.setCancelled(true);
    }

    private ItemStack heldItem(Player player) {
        PlayerInventory inv = player.getInventory();
        return inv.getItemInMainHand().getType() == org.bukkit.Material.AIR
                ? inv.getItemInOffHand()
                : inv.getItemInMainHand();
    }
}
