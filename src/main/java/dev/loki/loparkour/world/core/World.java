package dev.loki.loparkour.world.core;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;
import dev.lolib.worlds.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class World {

    private World() {
    }

    private static String name;
    private static org.bukkit.World world;

    /**
     * Creates a new world and sets all according settings in it.
     */
    public static void create() {
        name = Config.CONFIG.getString("world.name");
        if (!isValidWorldFolderName(name)) {
            LoParkour.getPlugin().getLogger().severe(
                    "Invalid world.name in config (use only letters, digits, '_' and '-', no path separators).");
            return;
        }

        var world = Bukkit.getWorld(name);

        if (!Config.CONFIG.getBoolean("joining")) {
            return;
        }

        if (world != null) {
            LoParkour.getPlugin().getLogger().warning(
                    "Crash detected! The parkour world loading twice is not usual behaviour. " +
                    "This only happens after a server crash.");
            World.world = world; // ensure static field is populated
        }

        if (Config.CONFIG.getBoolean("world.delete-on-reload")) {
            deleteWorld();
        }

        createWorld();
        setup();
    }

    private static void createWorld() {
        LoParkour.log("Creating Spigot world");

        try {
            WorldCreator creator = new WorldCreator(name)
                    .generateStructures(false)
                    .type(WorldType.NORMAL)
                    .generator(VoidGenerator.getGenerator()) // to fix No keys in MapLayer etc.
                    .environment(org.bukkit.World.Environment.NORMAL);

            world = Bukkit.createWorld(creator);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while trying to create the parkour world — check world.name — " + ex.getMessage());
        }
    }

    private static void setup() {
        if (world == null) {
            LoParkour.getPlugin().getLogger().severe("Cannot setup world rules — world is null (createWorld failed).");
            return;
        }
        LoParkour.log("Initializing world rules");

        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(10_000_000);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setClearWeatherDuration(1000000);
        world.setAutoSave(false);
    }

    /**
     * Deletes the world.
     */
    public static void delete() {
        if (!Config.CONFIG.getBoolean("world.delete-on-reload") || !Config.CONFIG.getBoolean("joining")) {
            return;
        }
        LoParkour.log("Deleting world");

        org.bukkit.World w = getWorld();
        if (w != null) {
            w.getPlayers().forEach(player -> player.kickPlayer("Server is restarting"));
        }

        deleteWorld();
    }

    private static void deleteWorld() {
        if (!isValidWorldFolderName(name)) {
            LoParkour.getPlugin().getLogger().severe("Refusing to delete world: invalid world name.");
            return;
        }

        LoParkour.log("Deleting Spigot world");

        File folder = worldFolder();
        if (!folder.exists()) {
            return;
        }

        Bukkit.unloadWorld(name, false);

        try (Stream<Path> files = Files.walk(folder.toPath())) {
            files.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while trying to delete the parkour world — " + ex.getMessage());
        }
    }

    private static File worldFolder() {
        return new File(Bukkit.getWorldContainer(), name);
    }

    /**
     * Prevents path traversal and accidental deletion outside the server world container.
     */
    public static boolean isValidWorldFolderName(String n) {
        if (n == null || n.isBlank() || n.length() > 64) {
            return false;
        }
        if (n.contains("..") || n.contains("/") || n.contains("\\")) {
            return false;
        }
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * @return the name of the parkour world.
     */
    public static String getName() {
        return name;
    }

    /**
     * @return the Bukkit world wherein LoParkour is currently active.
     */
    public static org.bukkit.World getWorld() {
        return world;
    }
}
