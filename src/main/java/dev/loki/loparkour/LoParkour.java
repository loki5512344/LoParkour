package dev.loki.loparkour;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.hook.HoloHook;
import dev.loki.loparkour.hook.PAPIHook;
import dev.loki.loparkour.mode.DefaultMode;
import dev.loki.loparkour.mode.ElytraMode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.mode.SpectatorMode;
import dev.loki.loparkour.mode.SpeedrunMode;
import dev.loki.loparkour.mode.RaceMode;
import dev.loki.loparkour.mode.CoopMode;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.player.UserRegistry;
import dev.loki.loparkour.reward.Rewards;
import dev.loki.loparkour.schematic.lpschem.LPSchematicManager;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.world.World;
import dev.lolib.core.LoPlugin;
import dev.lolib.scheduler.Scheduler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class of LoParkour
 *
 * @author loki
 */
public final class LoParkour extends LoPlugin {

    public static final String NAME = "&#FF6464<bold>LoParkour<reset>";
    public static final String PREFIX = NAME + " &#404040» &#A0A0A0";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    
    public static Gson getGson() {
        return gson;
    }
    private static LoParkour instance;
    private static LPSchematicManager schematicManager;

    @Nullable
    private static PAPIHook placeholderHook;

    public static void log(String message) {
        if (Config.CONFIG.getBoolean("debug")) {
            LoParkour.getPlugin().getLogger().info("[Debug] " + message);
        }
    }

    /**
     * @param child The file name.
     * @return A file from within the plugin folder.
     */
    public static File getInFolder(String child) {
        return new File(instance.getDataFolder(), child);
    }

    /**
     * @return This plugin's {@link Logging} instance.
     */
    

    /**
     * @return The plugin instance.
     */
    public static LoParkour getPlugin() {
        return instance;
    }

    @Nullable
    public static PAPIHook getPlaceholderHook() {
        return placeholderHook;
    }

    public static LPSchematicManager getSchematicManager() {
        return schematicManager;
    }

    

    @Override
    public void enable() {
        instance = this; // must be first — Config enum accesses instance inside reload()
        loadConfigs();
        loadSchematics();
        registerModes();
        registerHooks();
        setupWorld();
        registerEventsAndCommands();
        setupMetrics();
    }

    private void loadConfigs() {
        Config.reload(true);
    }

    private void loadSchematics() {
        schematicManager = new LPSchematicManager();
        
        // Create default island schematic if it doesn't exist
        File schematicsFolder = getInFolder("schematics-new");
        File islandFile = new File(schematicsFolder, "island.lpschem");
        
        if (!islandFile.exists()) {
            try {
                createDefaultIslandSchematic(islandFile);
                getLogger().info("Created default island schematic");
            } catch (Exception ex) {
                getLogger().severe("Failed to create default island schematic: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        
        schematicManager.loadAll();
    }
    
    private void createDefaultIslandSchematic(File file) throws Exception {
        // Create a larger 9x5x9 island for better visibility and safety
        List<String> palette = new ArrayList<>();
        palette.add("minecraft:air");
        palette.add("minecraft:stone");
        palette.add("minecraft:diamond_block");
        palette.add("minecraft:emerald_block");
        
        int width = 9, height = 5, length = 9;
        int[] blocks = new int[width * height * length];
        
        // Fill everything with air first
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = 0;
        }
        
        // Create a solid stone platform at y=1 (7x7 platform)
        for (int z = 1; z < 8; z++) {
            for (int x = 1; x < 8; x++) {
                int index = x + (z * width) + (1 * width * length);
                blocks[index] = 1; // stone
            }
        }
        
        // Place diamond block (player spawn) at center
        int centerX = 4, centerZ = 4;
        int diamondIndex = centerX + (centerZ * width) + (1 * width * length);
        blocks[diamondIndex] = 2; // diamond_block - player spawns here
        
        // Place emerald block (parkour start) to the right of spawn
        int emeraldIndex = (centerX + 1) + (centerZ * width) + (1 * width * length);
        blocks[emeraldIndex] = 3; // emerald_block - parkour starts here
        
        dev.loki.loparkour.schematic.lpschem.SchematicMetadata metadata = 
            new dev.loki.loparkour.schematic.lpschem.SchematicMetadata("island", "LoParkour", 0.0);
        metadata.addTag("spawn");
        metadata.addTag("island");
        
        dev.loki.loparkour.schematic.lpschem.SchematicDimensions dimensions = 
            new dev.loki.loparkour.schematic.lpschem.SchematicDimensions(width, height, length);
        
        // Markers point to the blocks where player spawns and parkour begins
        dev.loki.loparkour.schematic.lpschem.SchematicMarkers markers = 
            new dev.loki.loparkour.schematic.lpschem.SchematicMarkers(
                new dev.loki.loparkour.schematic.lpschem.SchematicMarkers.Vector3i(centerX, 1, centerZ),
                new dev.loki.loparkour.schematic.lpschem.SchematicMarkers.Vector3i(centerX + 1, 1, centerZ)
            );
        
        dev.loki.loparkour.schematic.lpschem.LPSchematic schematic = 
            new dev.loki.loparkour.schematic.lpschem.LPSchematic(metadata, dimensions, palette, blocks, markers);
        
        schematic.save(file);
    }

    private void registerModes() {
        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());
        Registry.register(new SpeedrunMode());
        Registry.register(new ElytraMode());
        Registry.register(new RaceMode());
        Registry.register(new CoopMode());
        Modes.init();
    }

    private void registerHooks() {
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().info("Registered Holographic Displays hook");
            HoloHook.init();
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("Registered PlaceholderAPI hook");
            placeholderHook = new PAPIHook();
            placeholderHook.register();
        }
        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getLogger().info("Registered BungeeCord hook");
        }
    }

    private void setupWorld() {
        if (!Config.CONFIG.getBoolean("joining")) {
            return;
        }

        // World creation MUST happen on the main thread (Bukkit API requirement)
        try {
            loLogger().info("Creating parkour world...");
            World.create();
            loLogger().info("Parkour world created successfully!");
        } catch (Exception ex) {
            loLogger().error("Failed to create parkour world", ex);
        }
    }

    private void registerEventsAndCommands() {
        dev.lolib.gui.GUIManager.init(this); // required for LoLib GUI click handling

        var pm = getServer().getPluginManager();
        pm.registerEvents(new dev.loki.loparkour.listener.PlayerConnectionListener(), this);
        pm.registerEvents(new dev.loki.loparkour.listener.ParkourRestrictionListener(), this);
        pm.registerEvents(new dev.loki.loparkour.listener.SchematicWandListener(), this);

        LoParkourCommand cmd = new LoParkourCommand();
        getCommand("LoParkour").setExecutor(cmd);
        getCommand("LoParkour").setTabCompleter(cmd);
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 29754);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_rewards", () -> Boolean.toString(Rewards.REWARDS_ENABLED)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Locales.locales.size())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () -> {
            int joins = UserRegistry.getJoinCount();
            return joins;
        }));
    }

    @Override
    public void disable() {
        try {
            for (ParkourUser user : ParkourUser.getUsers()) {
                ParkourUser.leave(user);
            }

            // Save leaderboards for all modes
            if (Modes.DEFAULT != null && Modes.DEFAULT.getLeaderboard() != null) {
                Modes.DEFAULT.getLeaderboard().write(false);
            }
            if (Modes.SPEEDRUN != null && Modes.SPEEDRUN.getLeaderboard() != null) {
                Modes.SPEEDRUN.getLeaderboard().write(false);
            }
            if (Modes.ELYTRA != null && Modes.ELYTRA.getLeaderboard() != null) {
                Modes.ELYTRA.getLeaderboard().write(false);
            }
            if (Modes.RACE != null && Modes.RACE.getLeaderboard() != null) {
                Modes.RACE.getLeaderboard().write(false);
            }
            if (Modes.COOP != null && Modes.COOP.getLeaderboard() != null) {
                Modes.COOP.getLeaderboard().write(false);
            }

            Storage.close();
            World.delete();
        } catch (Throwable ex) {
            // Log errors instead of silently ignoring them
            getLogger().severe("Error during plugin disable: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
