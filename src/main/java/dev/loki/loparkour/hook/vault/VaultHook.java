package dev.loki.loparkour.hook.vault;

import dev.loki.loparkour.LoParkour;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private VaultHook() {}

    private static Economy economy;

    /**
     * Deposits amount to the bal of player.
     *
     * @param player The player.
     * @param amount The amount.
     */
    public static void deposit(Player player, double amount) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        if (economy == null) {
            RegisteredServiceProvider<Economy> service = Bukkit.getServicesManager().getRegistration(Economy.class);

            if (service == null) {
                LoParkour.getPlugin().getLogger().severe("Error while trying to fetch the Vault economy - No economy found");
                return;
            }

            economy = service.getProvider();
        }

        economy.depositPlayer(player, amount);
    }
}
