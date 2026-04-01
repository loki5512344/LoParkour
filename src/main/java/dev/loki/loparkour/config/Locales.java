package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.menu.ParkourOption;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.util.Item;
import dev.loki.loparkour.util.Materials;
import dev.lolib.scheduler.Scheduler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Locale system: loading, caching, validation, and access.
 */
public class Locales {

    private static final Map<String, FileConfiguration> locales = new HashMap<>();
    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("%[a-z]");

    // ── public API ────────────────────────────────────────────────────────────

    public static void init() {
        loadLocalesAsync();
    }

    @NotNull
    public static String getString(@NotNull Player player, @NotNull String path) {
        return getString(getPlayerLocale(player), path);
    }

    @NotNull
    public static String getString(@NotNull String locale, @NotNull String path) {
        return cachedValue(locale, c -> c.getString(path), "");
    }

    @NotNull
    public static List<String> getStringList(@NotNull String locale, @NotNull String path) {
        return cachedValue(locale, c -> c.getStringList(path), Collections.emptyList());
    }

    public static int getLocaleCount() {
        synchronized (locales) { return locales.size(); }
    }

    @NotNull
    public static Set<String> getLocaleKeys() {
        synchronized (locales) { return new HashSet<>(locales.keySet()); }
    }

    @NotNull
    public static Item getItem(@NotNull Player player, @NotNull String path, String... replace) {
        return getItem(getPlayerLocale(player), path, replace);
    }

    @NotNull
    public static Item getItem(@NotNull String locale, @NotNull String path, String... replace) {
        synchronized (locales) {
            if (locales.isEmpty()) return new Item(Material.STONE, "");
            FileConfiguration config = locales.get(locale);
            if (config == null) return new Item(Material.STONE, "");
            return buildItem(config, path, replace);
        }
    }

    // ── loading (was LocaleLoader) ────────────────────────────────────────────

    private static void loadLocalesAsync() {
        Plugin plugin = LoParkour.getPlugin();
        Scheduler.get(plugin).runAsync(() -> {
            try {
                FileConfiguration embedded = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(plugin.getResource("locales/en.yml"), StandardCharsets.UTF_8));

                Map<String, FileConfiguration> loaded = loadFromDisk(embedded);

                Scheduler.get(plugin).run(() -> {
                    synchronized (locales) {
                        locales.clear();
                        locales.putAll(loaded);
                    }
                    LoParkour.log("Locales reloaded successfully (" + loaded.size() + " locales)");
                });
            } catch (Exception ex) {
                plugin.getLogger().severe("Error while loading locale files: " + ex.getMessage());
            }
        });
    }

    @NotNull
    private static Map<String, FileConfiguration> loadFromDisk(@NotNull FileConfiguration embedded) throws Exception {
        Map<String, FileConfiguration> result = new HashMap<>();
        File folder = LoParkour.getInFolder("locales");
        ensureFolder(folder);

        try (Stream<Path> stream = Files.list(folder.toPath())) {
            stream.forEach(path -> {
                File file = path.toFile();
                if (!file.getName().endsWith(".yml")) return;

                String locale = file.getName().split("\\.")[0];
                LoParkour.log("Found locale " + locale);

                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                validateAndFix(embedded, config, file);
                result.put(locale, config);
            });
        }
        return result;
    }

    private static void ensureFolder(@NotNull File folder) {
        if (!folder.exists()) folder.mkdirs();
        String[] files = folder.list();
        if (files == null || files.length == 0) {
            Plugin plugin = LoParkour.getPlugin();
            plugin.saveResource("locales/en.yml", false);
            plugin.saveResource("locales/ru.yml", false);
        }
    }

    // ── validation (was LocaleValidator) ──────────────────────────────────────

    private static void validateAndFix(@NotNull FileConfiguration reference, @NotNull FileConfiguration locale, @NotNull File file) {
        List<String> refNodes = getChildren(reference);
        List<String> locNodes = getChildren(locale);

        boolean modified = false;
        for (String node : refNodes) {
            if (!locNodes.contains(node)) {
                LoParkour.log("Fixing missing config node %s in %s".formatted(node, file.getName()));
                locale.set(node, reference.get(node));
                modified = true;
            }
        }
        if (modified) {
            try { locale.save(file); }
            catch (IOException ex) {
                LoParkour.getPlugin().getLogger().severe(
                    "Error saving fixed locale %s — delete and restart — %s".formatted(file.getName(), ex.getMessage()));
            }
        }
        LoParkour.log("Validated locale " + file.getName());
    }

    @NotNull
    private static List<String> getChildren(@NotNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("");
        return section != null ? new ArrayList<>(section.getKeys(true)) : Collections.emptyList();
    }

    // ── cache access (was LocaleCache) ────────────────────────────────────────

    @NotNull
    private static <T> T cachedValue(@NotNull String locale, @NotNull Function<FileConfiguration, T> extractor, @NotNull T defaultValue) {
        synchronized (locales) {
            if (locales.isEmpty()) return defaultValue;
            FileConfiguration config = locales.get(locale);
            if (config == null) return defaultValue;
            try {
                T result = extractor.apply(config);
                return result != null ? result : defaultValue;
            } catch (Exception e) { return defaultValue; }
        }
    }

    // ── item builder ──────────────────────────────────────────────────────────

    @NotNull
    private static Item buildItem(@NotNull FileConfiguration config, @NotNull String path, String... replace) {
        String material = config.getString("%s.material".formatted(path), "STONE");
        String name     = applyReplacements(config.getString("%s.name".formatted(path), ""), replace);
        String lore     = applyReplacements(config.getString("%s.lore".formatted(path), ""), replace);

        Material mat = Materials.parse(material);
        if (mat == null) {
            LoParkour.getPlugin().getLogger().warning("Invalid material '%s' for locale path '%s', using STONE".formatted(material, path));
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
        return (loc != null && !loc.isBlank()) ? loc : "en";
    }
}
