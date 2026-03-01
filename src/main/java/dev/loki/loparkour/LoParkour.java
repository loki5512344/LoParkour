package dev.loki.loparkour;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.hook.HoloHook;
import dev.loki.loparkour.hook.PAPIHook;
import dev.loki.loparkour.mode.DefaultMode;
import dev.loki.loparkour.mode.ElytraMode;
import dev.loki.loparkour.mode.GravityShiftMode;
import dev.loki.loparkour.mode.HardcoreMode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.mode.SpectatorMode;
import dev.loki.loparkour.mode.SpeedrunMode;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.reward.Rewards;
import dev.loki.loparkour.schematic.lpschem.LPSchematicManager;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.world.World;
import dev.lolib.core.LoPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Main class of LoParkour
 *
 * @author loki
 */
public final class LoParkour extends LoPlugin {

    public static final String NAME = "&#FF6464<bold>LoParkour<reset>";
    public static final String PREFIX = NAME + " &#404040» &#A0A0A0";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
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
    public void onEnable() {
        instance = this;
        super.onEnable();
    }

    @Override
    public void enable() {
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
        schematicManager.loadAll();
    }

    private void registerModes() {
        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());
        Registry.register(new SpeedrunMode());
        Registry.register(new GravityShiftMode());
        Registry.register(new HardcoreMode());
        Registry.register(new ElytraMode());
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
        if (Config.CONFIG.getBoolean("joining")) {
            World.create();
        }
    }

    private void registerEventsAndCommands() {
        getServer().getPluginManager().registerEvents(new Events(), this);
        getCommand("LoParkour").setExecutor(new Command());
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 29754);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_rewards", () -> Boolean.toString(Rewards.REWARDS_ENABLED)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Locales.locales.size())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () -> {
            int joins = ParkourUser.joinCount;
            ParkourUser.joinCount = 0;
            return joins;
        }));
    }

    @Override
    public void disable() {
        try {
            for (ParkourUser user : ParkourUser.getUsers()) {
                ParkourUser.leave(user);
            }

            // write all LoParkour gamemodes
            Modes.DEFAULT.getLeaderboard().write(false);

            Storage.close();
            World.delete();
        } catch (Throwable ignored) {

        }
    }
}
