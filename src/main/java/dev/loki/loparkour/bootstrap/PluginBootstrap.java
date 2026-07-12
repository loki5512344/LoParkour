package dev.loki.loparkour.bootstrap;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.adaptive.bootstrap.AdaptiveServices;
import dev.loki.loparkour.api.core.Registry;
import dev.loki.loparkour.command.LoParkourCommand;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.hook.holo.HoloHook;
import dev.loki.loparkour.hook.papi.PAPIHook;
import dev.loki.loparkour.listener.gameplay.ParkourRestrictionListener;
import dev.loki.loparkour.listener.schematic.SchematicWandListener;
import dev.loki.loparkour.listener.player.PlayerConnectionListener;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.mode.impl.CoopMode;
import dev.loki.loparkour.mode.impl.DefaultMode;
import dev.loki.loparkour.mode.impl.GravityShiftMode;
import dev.loki.loparkour.mode.impl.InvisibleBarrierMode;
import dev.loki.loparkour.mode.impl.RaceMode;
import dev.loki.loparkour.mode.impl.SpectatorMode;
import dev.loki.loparkour.mode.impl.SpeedrunMode;
import dev.loki.loparkour.reward.core.Rewards;
import dev.loki.loparkour.schematic.core.SchematicManager;
import dev.loki.loparkour.world.core.World;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.jetbrains.annotations.NotNull;

/**
 * Wires plugin subsystems during {@link LoParkour#enable()} and {@link LoParkour#disable()}.
 */
public final class PluginBootstrap {

    private PluginBootstrap() {
    }

    public static void enable(@NotNull LoParkour plugin) {
        Config.reload(true);
        AdaptiveServices.init(plugin);
        loadSchematics(plugin);
        registerModes();
        registerHooks(plugin);
        setupWorld(plugin);
        registerEventsAndCommands(plugin);
        setupMetrics(plugin);
    }

    public static void disable(@NotNull LoParkour plugin) {
        plugin.runShutdownSequence();
    }

    private static void loadSchematics(@NotNull LoParkour plugin) {
        extractSchematicDocs(plugin);
        SchematicManager manager = new SchematicManager();
        manager.loadAll();
        plugin.setSchematicManager(manager);
    }

    private static void extractSchematicDocs(@NotNull LoParkour plugin) {
        java.io.File doc = LoParkour.getInFolder("docs/SCHEMATICS.md");
        if (!doc.exists()) {
            doc.getParentFile().mkdirs();
            plugin.saveResource("docs/SCHEMATICS.md", false);
        }
    }

    private static void registerModes() {
        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());
        Registry.register(new SpeedrunMode());
        Registry.register(new RaceMode());
        Registry.register(new InvisibleBarrierMode());
        Registry.register(new CoopMode());
        Registry.register(new GravityShiftMode());
        Modes.init();
    }

    private static void registerHooks(@NotNull LoParkour plugin) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            plugin.getLogger().info("Registered Holographic Displays hook");
            HoloHook.init();
        }
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().info("Registered PlaceholderAPI hook");
            PAPIHook hook = new PAPIHook();
            hook.register();
            plugin.setPlaceholderHook(hook);
        }
        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
            plugin.getLogger().info("Registered BungeeCord hook");
        }
    }

    private static void setupWorld(@NotNull LoParkour plugin) {
        if (!Config.CONFIG.getBoolean("joining")) {
            return;
        }
        try {
            plugin.loLogger().info("Creating parkour world...");
            World.create();
            plugin.loLogger().info("Parkour world created successfully!");
        } catch (Exception ex) {
            plugin.loLogger().error("Failed to create parkour world", ex);
        }
    }

    private static void registerEventsAndCommands(@NotNull LoParkour plugin) {
        dev.lolib.gui.GUIManager.init(plugin);

        var pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(), plugin);
        pm.registerEvents(new ParkourRestrictionListener(), plugin);
        pm.registerEvents(new SchematicWandListener(), plugin);
        LoParkourCommand cmd = new LoParkourCommand();
        plugin.getCommand("LoParkour").setExecutor(cmd);
        plugin.getCommand("LoParkour").setTabCompleter(cmd);
    }

    private static void setupMetrics(@NotNull LoParkour plugin) {
        Metrics metrics = new Metrics(plugin, 29754);
        metrics.addCustomChart(new SimplePie("using_sql", () -> Boolean.toString(Option.SQL)));
        metrics.addCustomChart(new SimplePie("using_rewards", () -> Boolean.toString(Rewards.REWARDS_ENABLED)));
        metrics.addCustomChart(new SimplePie("locale_count", () -> Integer.toString(Locales.getLocaleCount())));
        metrics.addCustomChart(new SingleLineChart("player_joins", () ->
                dev.loki.loparkour.player.service.UserRegistry.getJoinCount()));
    }
}
