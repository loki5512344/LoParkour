package dev.loki.loparkour.schematic.core;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.schematic.schem.SchemLoader;
import dev.loki.loparkour.config.core.Config;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loads {@code .nbt}, {@code .schem}, and {@code .schematic} files from {@code plugins/LoParkour/schematics/}.
 * Difficulty per structure is configured in {@code schematics/schematics.yml}.
 */
public final class SchematicManager {

    private static final String PARKOUR_PREFIX = "parkour-";

    private final File folder;
    private final Map<String, ParkourSchematic> loaded = new HashMap<>();

    public SchematicManager() {
        this.folder = LoParkour.getInFolder("schematics");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public void loadAll() {
        loaded.clear();

        int nbtCount = loadNbtFiles();
        int schemCount = loadSchemFiles();

        if (nbtCount + schemCount == 0) {
            LoParkour.log("No .nbt / .schem files in " + folder.getAbsolutePath());
            return;
        }

        LoParkour.log("Loaded " + loaded.size() + " schematic(s) (" + nbtCount + " nbt, " + schemCount + " schem)");
    }

    public void reload() {
        loadAll();
    }

    @Nullable
    public ParkourSchematic get(@NotNull String id) {
        return loaded.get(id.toLowerCase(Locale.ROOT));
    }

    @NotNull
    public Map<String, ParkourSchematic> getAll() {
        return new HashMap<>(loaded);
    }

    /**
     * Picks a random structure with difficulty at most {@code maxDifficulty}
     * (player schematic setting: higher value allows harder structures).
     */
    @Nullable
    public ParkourSchematic pick(double maxDifficulty) {
        List<ParkourSchematic> eligible = new ArrayList<>();
        for (ParkourSchematic schematic : loaded.values()) {
            if (schematic.getDifficulty() <= maxDifficulty + 1e-9) {
                eligible.add(schematic);
            }
        }
        if (eligible.isEmpty()) {
            return null;
        }
        return eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
    }

    private int loadNbtFiles() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".nbt"));
        if (files == null || files.length == 0) {
            return 0;
        }

        StructureManager structureManager = Bukkit.getStructureManager();
        int ok = 0;
        for (File file : files) {
            String stem = stem(file, ".nbt");
            try {
                Structure structure = structureManager.loadStructure(file);
                register(stem, ParkourSchematic.fromNbt(stem, resolveDifficulty(stem), structure));
                ok++;
            } catch (IOException e) {
                LoParkour.getPlugin().getLogger().severe("Failed to load structure: " + file.getName());
                e.printStackTrace();
            }
        }
        return ok;
    }

    private int loadSchemFiles() {
        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".schem") || lower.endsWith(".schematic");
        });
        if (files == null || files.length == 0) {
            return 0;
        }

        int ok = 0;
        for (File file : files) {
            String stem = stem(file, file.getName().toLowerCase(Locale.ROOT).endsWith(".schematic")
                    ? ".schematic"
                    : ".schem");
            try {
                Clipboard clipboard = SchemLoader.load(file);
                register(stem, ParkourSchematic.fromSchem(stem, resolveDifficulty(stem), clipboard));
                ok++;
            } catch (IOException e) {
                LoParkour.getPlugin().getLogger().severe("Failed to load schematic: " + file.getName());
                e.printStackTrace();
            }
        }
        return ok;
    }

    private void register(@NotNull String stem, @NotNull ParkourSchematic schematic) {
        String key = stem.toLowerCase(Locale.ROOT);
        if (loaded.containsKey(key)) {
            LoParkour.getPlugin().getLogger().warning(
                    "Duplicate schematic id '" + stem + "' — keeping " + loaded.get(key).getFormat()
                            + ", skipping " + schematic.getFormat());
            return;
        }
        loaded.put(key, schematic);
    }

    @NotNull
    private static String stem(@NotNull File file, @NotNull String suffix) {
        String name = file.getName();
        return name.substring(0, name.length() - suffix.length());
    }

    private static double resolveDifficulty(@NotNull String fileStem) {
        ConfigurationSection section = Config.SCHEMATICS.fileConfiguration == null
                ? null
                : Config.SCHEMATICS.fileConfiguration.getConfigurationSection("difficulty");
        if (section == null) {
            return 0.5;
        }

        String key = configKey(fileStem);
        if (section.contains(key)) {
            return section.getDouble(key);
        }
        if (section.contains(fileStem)) {
            return section.getDouble(fileStem);
        }
        return 0.5;
    }

    @NotNull
    public static String configKey(@NotNull String fileStem) {
        if (fileStem.regionMatches(true, 0, PARKOUR_PREFIX, 0, PARKOUR_PREFIX.length())) {
            return fileStem.substring(PARKOUR_PREFIX.length());
        }
        return fileStem;
    }
}
