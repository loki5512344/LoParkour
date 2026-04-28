package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.util.Item;
import dev.loki.loparkour.util.Materials;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facade for locale system: loading, caching, and access.
 * Delegates to LocaleLoader and LocaleCache.
 */
public class Locales {

    private static final LocaleCache cache = new LocaleCache();
    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("%[a-z]");

    // ── Initialization ────────────────────────────────────────────────────────

    public static void init() {
        Map<String, FileConfiguration> loaded = LocaleLoader.loadAll();
        cache.setLocales(loaded);
        LoParkour.log("Locales loaded successfully (" + loaded.size() + " locales)");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    @NotNull
    public static String getString(@NotNull Player player, @NotNull String path) {
        return getString(getPlayerLocale(player), path);
    }

    /**
     * Player: that player's locale. Console / command blocks: default language from config, or {@code en}.
     */
    @NotNull
    public static String getString(@NotNull CommandSender sender, @NotNull String path) {
        if (sender instanceof Player player) {
            return getString(player, path);
        }
        String locale = Option.OPTIONS_DEFAULTS != null
                ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG)
                : null;
        if (locale == null || locale.isBlank()) {
            locale = "en";
        }
        return getString(locale, path);
    }

    @NotNull
    public static String getString(@NotNull String locale, @NotNull String path) {
        return cache.cachedValue(locale, c -> c.getString(path), "");
    }

    @NotNull
    public static List<String> getStringList(@NotNull String locale, @NotNull String path) {
        return cache.cachedValue(locale, c -> c.getStringList(path), Collections.emptyList());
    }

    public static int getLocaleCount() {
        return cache.getLocaleCount();
    }

    @NotNull
    public static Set<String> getLocaleKeys() {
        return cache.getLocaleKeys();
    }

    @NotNull
    public static Item getItem(@NotNull Player player, @NotNull String path, String... replace) {
        return getItem(getPlayerLocale(player), path, replace);
    }

    @NotNull
    public static Item getItem(@NotNull String locale, @NotNull String path, String... replace) {
        if (cache.isEmpty()) {
            LoParkour.getPlugin().getLogger().warning("Locales are empty! Returning STONE item for path: " + path);
            return new Item(Material.STONE, "");
        }
        FileConfiguration config = cache.getLocale(locale);
        if (config == null) {
            LoParkour.getPlugin().getLogger().warning(
                "Locale '" + locale + "' not found! Available: " + cache.getLocaleKeys() + ". Returning STONE for path: " + path);
            return new Item(Material.STONE, "");
        }
        return buildItem(config, path, replace);
    }

    // ── Item builder ──────────────────────────────────────────────────────────

    @NotNull
    private static Item buildItem(@NotNull FileConfiguration config, @NotNull String path, String... replace) {
        String materialPath = "%s.material".formatted(path);
        String namePath = "%s.name".formatted(path);
        String lorePath = "%s.lore".formatted(path);

        String material = config.getString(materialPath, "STONE");
        String name = applyReplacements(config.getString(namePath, ""), replace);
        String lore = applyReplacements(config.getString(lorePath, ""), replace);

        // Debug logging
        if ("STONE".equals(material)) {
            LoParkour.getPlugin().getLogger().warning("Material defaulted to STONE for path: " + path);
            LoParkour.getPlugin().getLogger().warning("  Tried: " + materialPath + " = " + config.getString(materialPath));
            LoParkour.getPlugin().getLogger().warning("  Available keys: " + config.getKeys(true));
        }

        Material mat = Materials.parse(material);
        if (mat == null) {
            LoParkour.getPlugin().getLogger().warning(
                "Invalid material '%s' for locale path '%s', using STONE".formatted(material, path));
            mat = Material.STONE;
        }

        Item item = new Item(mat, name);
        if (!lore.isEmpty()) item.lore(lore.split("\\|\\|"));
        return item;
    }

    @NotNull
    private static String applyReplacements(@NotNull String text, String... replacements) {
        if (replacements.length == 0) return text;
        String result = text;
        int idx = 0;
        Matcher matcher = REPLACEMENT_PATTERN.matcher(result);
        while (matcher.find() && idx < replacements.length) {
            result = result.replaceFirst(matcher.group(), replacements[idx++]);
        }
        return result;
    }

    @NotNull
    private static String getPlayerLocale(@NotNull Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String loc = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        // Fix legacy Boolean "true" values from old config parsing
        if ("true".equals(loc) || "false".equals(loc)) {
            loc = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);
            if (user != null) {
                user.locale = loc; // Update user's locale to fixed value
            }
        }

        return (loc != null && !loc.isBlank()) ? loc : "en";
    }
}
