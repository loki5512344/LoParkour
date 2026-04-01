package dev.loki.loparkour.command;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.player.data.InventoryData;
import dev.loki.loparkour.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles admin-only /LoParkour sub-commands:
 * forcejoin, forceleave, reset, recoverinventory.
 */
public class AdminCommandHandler {

    private static final String COOLDOWN_HINT = "<gray>Please wait before using this again.";

    private final PlayerCommandHandler base;

    public AdminCommandHandler(PlayerCommandHandler base) {
        this.base = base;
    }

    public void handle(@NotNull String arg1, @NotNull String arg2,
                       @NotNull CommandSender sender, @Nullable Player player) {

        if (!sender.hasPermission(ParkourOption.ADMIN.permission)) return;

        switch (arg1.toLowerCase()) {
            case "forcejoin"  -> handleForceJoin(arg2, sender);
            case "forceleave" -> handleForceLeave(arg2, sender);
            case "reset"      -> handleReset(arg2, sender);
            case "recoverinventory" -> handleRecoverInventory(arg2, sender);
        }

        if (player == null) return;

        if (arg1.equals("schematic")) {
            SchematicCommandHandler.handleSubcommand(arg2, sender, player, base);
        }
    }

    // ── forcejoin ──────────────────────────────────────────────────────────────

    private void handleForceJoin(String target, CommandSender sender) {
        if (!base.cooldown(sender, "forcejoin", 2500, Locales.getString(sender, "admin.cooldown"))) return;

        if (target.equalsIgnoreCase("everyone")) {
            Bukkit.getOnlinePlayers().forEach(p -> Modes.DEFAULT.create(p));
            send(sender, LoParkour.PREFIX + "Force joined everyone!");
            return;
        }
        if (target.equalsIgnoreCase("nearest")) {
            Player closest = findNearest(sender);
            if (closest == null) return;
            Modes.DEFAULT.create(closest);
            send(sender, LoParkour.PREFIX + "Force joined " + closest.getName() + "!");
            return;
        }
        Player other = Bukkit.getPlayer(target);
        if (other == null) { send(sender, LoParkour.PREFIX + "Player not online!"); return; }
        Modes.DEFAULT.create(other);
    }

    // ── forceleave ─────────────────────────────────────────────────────────────

    private void handleForceLeave(String target, CommandSender sender) {
        if (!base.cooldown(sender, "forceleave", 2500, Locales.getString(sender, "admin.cooldown"))) return;

        if (target.equalsIgnoreCase("everyone")) {
            ParkourPlayer.getPlayers().forEach(ParkourUser::leave);
            send(sender, LoParkour.PREFIX + "Force kicked everyone!");
            return;
        }
        Player other = Bukkit.getPlayer(target);
        if (other == null) { send(sender, LoParkour.PREFIX + "Player not online!"); return; }
        ParkourUser user = ParkourUser.getUser(other);
        if (user == null) { send(sender, LoParkour.PREFIX + "Player isn't playing!"); return; }
        ParkourUser.leave(user);
    }

    // ── reset ──────────────────────────────────────────────────────────────────

    private void handleReset(String target, CommandSender sender) {
        if (!base.cooldown(sender, "reset", 2500, COOLDOWN_HINT)) return;

        if (target.equalsIgnoreCase("everyone")) {
            Registry.getModes().stream()
                .map(Mode::getLeaderboard)
                .filter(lb -> lb != null)
                .forEach(lb -> { lb.resetAll(); lb.write(true); });
            send(sender, LoParkour.PREFIX + "Reset all high scores.");
            return;
        }

        UUID uuid = resolveUUID(target);
        String name = resolvePlayerName(target, uuid);

        Registry.getModes().stream()
            .map(Mode::getLeaderboard)
            .filter(lb -> lb != null)
            .forEach(lb -> { lb.remove(uuid); lb.write(true); });

        send(sender, LoParkour.PREFIX + "Reset high score of " + name + ".");
    }

    // ── recoverinventory ───────────────────────────────────────────────────────

    private void handleRecoverInventory(String target, CommandSender sender) {
        if (!base.cooldown(sender, "recoverinventory", 2500, Locales.getString(sender, "admin.cooldown"))) return;
        Player other = Bukkit.getPlayer(target);
        if (other == null) { send(sender, LoParkour.PREFIX + "Player not online!"); return; }

        new InventoryData(other).load(result -> {
            if (result != null) {
                send(sender, LoParkour.PREFIX + "Recovered inventory of " + other.getName() + ".");
            } else {
                send(sender, LoParkour.PREFIX + "<red>No saved inventory found for " + other.getName() + ".");
            }
        });
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private @Nullable Player findNearest(CommandSender sender) {
        Location from = sender instanceof Player p ? p.getLocation()
                : sender instanceof BlockCommandSender b ? b.getBlock().getLocation()
                : null;
        if (from == null || from.getWorld() == null) return null;

        return from.getWorld().getPlayers().stream()
            .min((a, b) -> Double.compare(a.getLocation().distance(from), b.getLocation().distance(from)))
            .orElse(null);
    }

    @SuppressWarnings("deprecation") // Bukkit: name-based OfflinePlayer lookup (admin-only)
    private UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        if (input.contains("-")) {
            try {
                return UUID.fromString(input);
            } catch (IllegalArgumentException ignored) {
                // fall through to offline lookup by name
            }
        }
        return Bukkit.getOfflinePlayer(input).getUniqueId();
    }

    private String resolvePlayerName(String input, UUID uuid) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : input;
    }

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(ColorUtil.color(msg));
    }
}
