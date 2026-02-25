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
import dev.efnilite.vilib.ViPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.util.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Main class of LoParkour
 *
 * @author loki
 */
public final class LoParkour extends ViPlugin {

    public static final String NAME = "&#FF6464<bold>LoParkour<reset>";
    public static final String PREFIX = NAME + " &#404040» &#A0A0A0";

    private static Logging logging;
    private static LoParkour instance;
    private static LPSchematicManager schematicManager;

    @Nullable
    private static PAPIHook placeholderHook;

    public static void log(String message) {
        if (Config.CONFIG.getBoolean("debug")) {
            logging.info("[Debug] " + message);
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
    public static Logging logging() {
        return logging;
    }

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
    public void onLoad() {
        instance = this;
        logging = new Logging(this);
    }

    @Override
    public void enable() {

        // ----- Configurations -----

        Config.reload(true);

        // ----- Schematics -----

        schematicManager = new LPSchematicManager();
        schematicManager.loadAll();

        // ----- Registry -----

        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());
        Registry.register(new SpeedrunMode());
        Registry.register(new GravityShiftMode());
        Registry.register(new HardcoreMode());
        Registry.register(new ElytraMode());

        Modes.init();
        Menu.init(this);

        // hook with hd / papi after gamemode leaderboards have initialized
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            logging.info("Registered Holographic Displays hook");
            HoloHook.init();
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging.info("Registered PlaceholderAPI hook");
            placeholderHook = new PAPIHook();
            placeholderHook.register();
        }

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            logging.info("Registered BungeeCord hook");
        }

        // ----- Worlds -----

        if (Config.CONFIG.getBoolean("joining")) {
            World.create();
        }

        // ----- Events -----

        registerListener(new Events());
        registerCommand("LoParkour", new Command());

        // ----- Metrics -----

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
