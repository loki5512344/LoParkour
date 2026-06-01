package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.player.core.ParkourPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Elytra loadout and launch helpers.
 */
public final class ElytraPlayerKit {

    private ElytraPlayerKit() {
    }

    public static void giveElytraAndFireworks(@NotNull ParkourPlayer player, @NotNull ElytraConfig config) {
        Player p = player.player;
        p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        p.getInventory().addItem(makeFirework(config.getStartingFireworks()));
    }

    public static void launch(@NotNull ParkourPlayer player, @NotNull Location spawn) {
        Player p = player.player;
        Location launchPos = spawn.clone().add(0, 10, 0);
        p.teleport(launchPos);
        p.setGliding(true);
        p.setVelocity(new Vector(1, 0, 0).normalize().multiply(1.5));
    }

    public static void spawnBoostFirework(@NotNull Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }

    @NotNull
    public static ItemStack makeFirework(int amount) {
        ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET, amount);
        FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
        if (meta != null) {
            meta.setPower(1);
            firework.setItemMeta(meta);
        }
        return firework;
    }
}
