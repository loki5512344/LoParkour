package dev.loki.loparkour.menu.settings;

import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.style.core.Style;
import dev.loki.loparkour.util.text.ColorUtil;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ParkourSettingsMenu extends LPMenu {

    public void open(@NotNull ParkourPlayer user) {
        if (user == null) return;
        open(user.player);
    }

    @Override
    public void open(@NotNull Player player) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp == null) return;

        String locale = pp.locale;
        String title = Locales.getString(locale, "settings.name");

        baseGui(title, 4)
                .setItem(10, toggleItem(locale, "particles", pp.particles), e -> {
                    pp.particles = !pp.particles;
                    pp.updateGeneratorSettings(pp.session.generator);
                    open(pp);
                })
                .setItem(11, toggleItem(locale, "sound", pp.sound), e -> {
                    pp.sound = !pp.sound;
                    pp.updateGeneratorSettings(pp.session.generator);
                    open(pp);
                })
                .setItem(12, toggleItem(locale, "special_blocks", pp.useSpecialBlocks), e -> {
                    if (pp.session.generator.state.score == 0) {
                        pp.useSpecialBlocks = !pp.useSpecialBlocks;
                        pp.updateGeneratorSettings(pp.session.generator);
                    }
                    open(pp);
                })
                .setItem(13, toggleItem(locale, "fall_message", pp.showFallMessage), e -> {
                    pp.showFallMessage = !pp.showFallMessage;
                    pp.updateGeneratorSettings(pp.session.generator);
                    open(pp);
                })
                .setItem(14, toggleItem(locale, "scoreboard", pp.showScoreboard), e -> {
                    pp.showScoreboard = !pp.showScoreboard;
                    pp.updateGeneratorSettings(pp.session.generator);
                    open(pp);
                })
                .setItem(19, styleItem(pp), e -> openStyleMenu(pp))
                .setItem(20, schemDiffItem(pp), e -> {
                    if (pp.session.generator.state.score == 0) {
                        List<Double> diffs = List.of(0.0, 0.25, 0.5, 0.75, 1.0);
                        int idx = diffs.indexOf(pp.schematicDifficulty);
                        pp.schematicDifficulty = diffs.get((idx + 1) % diffs.size());
                        pp.updateGeneratorSettings(pp.session.generator);
                    }
                    open(pp);
                })
                .setItem(31, closeButton(player), e -> Menus.SETTINGS.open(player))
                .open(player);
    }

    private ItemStack toggleItem(String locale, String key, boolean value) {
        Material mat = value ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String symbol = Locales.getString(locale, "settings.parkour_settings." + (value ? "enabled" : "disabled"));
        String name = Locales.getString(locale, "settings.parkour_settings.items." + key + ".name");
        String lore = Locales.getString(locale, "settings.parkour_settings.items." + key + ".lore");
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name + " §7[" + symbol + "§7]"));
            if (lore != null && !lore.isEmpty()) {
                List<String> loreLines = new ArrayList<>();
                for (String line : lore.split("\\|\\|")) {
                    loreLines.add(ColorUtil.color(line.replace("%s", symbol)));
                }
                meta.setLore(loreLines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack schemDiffItem(ParkourPlayer pp) {
        List<Double> diffs = List.of(0.0, 0.25, 0.5, 0.75, 1.0);
        List<String> values = Locales.getStringList(pp.locale, "settings.parkour_settings.items.schematics.values");
        int idx = Math.max(0, diffs.indexOf(pp.schematicDifficulty));
        String label = idx < values.size() ? values.get(idx) : String.valueOf(pp.schematicDifficulty);
        Material[] mats = {Material.RED_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.SKELETON_SKULL};
        ItemStack item = new ItemStack(idx < mats.length ? mats[idx] : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = Locales.getString(pp.locale, "settings.parkour_settings.items.schematics.name");
            String lore = Locales.getString(pp.locale, "settings.parkour_settings.items.schematics.lore");
            meta.setDisplayName(ColorUtil.color(name.replace("%s", label)));
            if (lore != null && !lore.isEmpty()) {
                List<String> loreLines = new ArrayList<>();
                for (String line : lore.split("\\|\\|")) {
                    loreLines.add(ColorUtil.color(line.replace("%s", label)));
                }
                meta.setLore(loreLines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack styleItem(ParkourPlayer pp) {
        String name = Locales.getString(pp.locale, "settings.parkour_settings.items.styles.item.name");
        String lore = Locales.getString(pp.locale, "settings.parkour_settings.items.styles.item.lore");
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name.replace("%s", pp.style)));
            if (lore != null && !lore.isEmpty()) {
                List<String> loreLines = new ArrayList<>();
                for (String line : lore.split("\\|\\|")) {
                    loreLines.add(ColorUtil.color(line.replace("%s", pp.style)));
                }
                meta.setLore(loreLines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openStyleMenu(ParkourPlayer pp) {
        Player player = pp.player;
        String locale = pp.locale;
        String title = Locales.getString(locale, "settings.parkour_settings.styles.name");

        InventoryGUI gui = baseGui(title, 3);

        int slot = 10;
        for (Style style : Registry.getStyles()) {
            if (slot > 16) break;
            String name = style.getName();
            String perm = ParkourOption.STYLES.permission + "." + name.toLowerCase().replace(" ", ".");
            if (Config.CONFIG.getBoolean("permissions.per-style") && !player.hasPermission(perm)) continue;

            ItemStack item = new ItemStack(style.getNext());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.color("&#A020F0" + name));
                item.setItemMeta(meta);
            }

            final Style s = style;
            gui = gui.setItem(slot++, item, e -> {
                pp.style = s.getName();
                pp.updateGeneratorSettings(pp.session.generator);
                open(pp);
            });
        }

        gui.open(player);
    }
}
