package dev.loki.loparkour.player.data;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;
import dev.lolib.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Saved hotbar/inventory slots for restore on leave. New files use YAML
 * (serializable ItemStacks); legacy Java-serialized files are still read once.
 */
public class InventoryData {

    private final File file;
    private final Player player;
    private Map<Integer, ItemStack> items = new HashMap<>();

    public InventoryData(Player player) {
        this.player = player;
        this.file = LoParkour.getInFolder("inventories/%s".formatted(player.getUniqueId()));
    }

    /**
     * Gives all items to the player.
     */
    public void apply() {
        player.getInventory().clear();
        items.forEach((slot, item) -> player.getInventory().setItem(slot, item));
    }

    /**
     * Loads inventory data from file.
     *
     * @param onFinish What to do when the async procedure has finished.
     */
    public void load(Consumer<@Nullable InventoryData> onFinish) {
        if (!file.exists()) {
            onFinish.accept(null);
            return;
        }

        Scheduler.get(LoParkour.getPlugin()).runAsync(() -> loadFile(onFinish));
    }

    private void loadFile(Consumer<@Nullable InventoryData> onFinish) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (!yaml.getKeys(false).isEmpty()) {
                loadFromYaml(yaml);
                onFinish.accept(this);
                return;
            }
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().warning(
                    "YAML inventory parse failed for %s, trying legacy format: %s".formatted(file.getName(), ex.getMessage()));
        }

        if (file.length() == 0) {
            onFinish.accept(null);
            return;
        }

        loadLegacy(onFinish);
    }

    private void loadFromYaml(YamlConfiguration yaml) {
        items = new HashMap<>();
        for (String key : yaml.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ItemStack stack = yaml.getItemStack(key);
                if (stack != null) {
                    items.put(slot, stack);
                }
            } catch (NumberFormatException ignored) {
                // skip non-slot keys
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLegacy(Consumer<@Nullable InventoryData> onFinish) {
        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            items = (Map<Integer, ItemStack>) stream.readObject();
            onFinish.accept(this);
        } catch (IOException | ClassNotFoundException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while reading inventory of %s from file %s".formatted(player.getName(), file.getName())
                            + " - " + ex.getMessage());
            onFinish.accept(null);
        }
    }

    /**
     * Saves the inventory to cache, so if the player leaves the player gets their items back
     *
     * @param toFile Whether the file should be updated.
     */
    public void save(boolean toFile) {
        int index = 0;

        Inventory inventory = this.player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                this.items.put(index, item);
            }

            index++;
        }

        String command = Config.CONFIG.getString("options.alt-inventory-saving-command");
        if (!command.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }

        if (toFile) {
            Scheduler.get(LoParkour.getPlugin()).runAsync(this::saveFile);
        }
    }

    private void saveFile() {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while creating file to save inventory of %s to file %s".formatted(player.getName(), file.getName())
                            + " - " + ex.getMessage());
        }

        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.options().header(" LoParkour inventory (YAML). Legacy binary saves are migrated on next save.");
            for (Map.Entry<Integer, ItemStack> e : items.entrySet()) {
                yaml.set(String.valueOf(e.getKey()), e.getValue());
            }
            yaml.save(file);
        } catch (IOException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while saving inventory of %s to file %s".formatted(player.getName(), file.getName())
                            + " - " + ex.getMessage());
        }
    }
}
