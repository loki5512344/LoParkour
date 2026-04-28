package dev.loki.loparkour.config.options;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.ConfigAccessor;
import dev.loki.loparkour.style.Style;
import dev.loki.loparkour.util.Materials;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Style initialization utilities.
 */
public class OptionStyles {

    public static Set<Style> initStyles(String path, FileConfiguration config,
                                        BiFunction<String, List<Material>, Style> fn) {
        var styles = new HashSet<Style>();
        ConfigAccessor accessor = new ConfigAccessor(config, "config.yml");

        for (String style : accessor.getChildren(path, false)) {
            styles.add(fn.apply(style,
                config.getStringList("%s.%s".formatted(path, style)).stream()
                    .map(name -> {
                        Material m = Materials.parse(name);
                        if (m == null) {
                            LoParkour.getPlugin().getLogger().severe(
                                "Invalid material %s in style %s".formatted(name, style));
                            return Material.STONE;
                        }
                        return m;
                    })
                    .toList()));
        }
        return styles;
    }
}
