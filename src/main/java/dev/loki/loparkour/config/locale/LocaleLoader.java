package dev.loki.loparkour.config.locale;

import dev.loki.loparkour.LoParkour;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads locale files from disk with validation and auto-fix.
 */
public class LocaleLoader {

    /**
     * Load all locale files from the locales/ folder.
     * Returns a map of locale name → FileConfiguration.
     */
    @NotNull
    public static Map<String, FileConfiguration> loadAll() {
        Plugin plugin = LoParkour.getPlugin();
        try {
            FileConfiguration embedded = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(plugin.getResource("locales/en.yml"), StandardCharsets.UTF_8));

            return loadFromDisk(embedded);
        } catch (Exception ex) {
            plugin.getLogger().severe("Error while loading locale files: " + ex.getMessage());
            return Collections.emptyMap();
        }
    }

    @NotNull
    private static Map<String, FileConfiguration> loadFromDisk(@NotNull FileConfiguration embedded) throws Exception {
        Map<String, FileConfiguration> result = new HashMap<>();
        File folder = LoParkour.getInFolder("locales");
        ensureFolder(folder);

        try (Stream<Path> stream = Files.list(folder.toPath())) {
            stream.forEach(path -> {
                File file = path.toFile();
                if (!file.getName().endsWith(".yml")) {
                    return;
                }

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

    /**
     * Validate a locale file against the reference (en.yml) and auto-fix missing keys.
     */
    private static void validateAndFix(@NotNull FileConfiguration reference,
                                       @NotNull FileConfiguration locale,
                                       @NotNull File file) {
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
            try {
                locale.save(file);
            } catch (IOException ex) {
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
}
