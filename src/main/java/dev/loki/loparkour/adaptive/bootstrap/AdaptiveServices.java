package dev.loki.loparkour.adaptive.bootstrap;

import dev.loki.loparkour.adaptive.core.AdaptiveDifficulty;
import dev.loki.loparkour.adaptive.core.MetricsCollector;
import dev.loki.loparkour.adaptive.model.AdaptiveConfig;
import dev.loki.loparkour.adaptive.model.SkillRating;
import dev.loki.loparkour.adaptive.storage.FileStatsStorage;
import dev.loki.loparkour.adaptive.storage.SQLStatsStorage;
import dev.loki.loparkour.adaptive.storage.StatsRepository;
import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.generator.core.model.Profile;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.storage.sql.StorageSQL;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Bootstrap for the adaptive difficulty subsystem.
 */
public final class AdaptiveServices {

    @Nullable
    private static Plugin plugin;
    @Nullable
    private static StatsRepository repository;
    @Nullable
    private static MetricsCollector metricsCollector;

    private AdaptiveServices() {
    }

    public static void init(@NotNull Plugin pluginInstance) {
        plugin = pluginInstance;
        AdaptiveConfig.init();
        if (!AdaptiveConfig.isEnabled()) {
            return;
        }

        Runnable setup = () -> startCollector(createRepository());
        if (Option.SQL) {
            Storage.runWhenReady(setup);
        } else {
            setup.run();
        }
    }

    public static void reload() {
        shutdownCollector();
        AdaptiveConfig.reload();
        if (plugin != null && AdaptiveConfig.isEnabled()) {
            Runnable setup = () -> startCollector(createRepository());
            if (Option.SQL) {
                Storage.runWhenReady(setup);
            } else {
                setup.run();
            }
        }
    }

    public static void attachToGenerator(@NotNull ParkourGenerator generator) {
        if (metricsCollector != null) {
            generator.registerEventListener(metricsCollector);
        }
    }

    /**
     * Applies adaptive difficulty weights to a profile being built for a generator.
     */
    public static void applyAdaptiveProfile(@NotNull ParkourGenerator generator, @NotNull Profile profile) {
        if (!AdaptiveConfig.isEnabled() || metricsCollector == null) {
            return;
        }

        double target = AdaptiveDifficulty.resolveTargetDifficulty(generator, AdaptiveServices::resolveSkillRating);
        AdaptiveDifficulty.applyToProfile(profile, target);
    }

    /**
     * Updates stored skill rating from current session metrics after a fall.
     */
    public static void persistRatingAfterFall(@NotNull ParkourPlayer player) {
        if (!AdaptiveConfig.isEnabled() || repository == null || metricsCollector == null) {
            return;
        }

        UUID uuid = player.getUUID();
        SkillRating session = AdaptiveDifficulty.analyzeSession(player, metricsCollector);
        SkillRating stored = repository.loadSkillRating(uuid);

        if (stored != null) {
            stored.updateRating(session.getRating(), AdaptiveConfig.getRatingWeight());
            repository.saveSkillRating(stored);
        } else {
            repository.saveSkillRating(session);
        }
    }

    /**
     * Rebuilds generator profile/chances from latest ratings (call once per fall event).
     */
    public static void refreshGeneratorDifficulty(@NotNull ParkourGenerator generator) {
        if (!AdaptiveConfig.isEnabled()) {
            return;
        }
        generator.profileManager.recalculateProfile();
    }

    public static void unloadPlayer(@NotNull UUID playerUuid) {
        if (metricsCollector != null) {
            metricsCollector.unloadPlayer(playerUuid);
        }
    }

    public static void shutdown() {
        shutdownCollector();
        repository = null;
    }

    @NotNull
    private static SkillRating resolveSkillRating(@NotNull ParkourPlayer player) {
        UUID uuid = player.getUUID();
        SkillRating stored = repository != null ? repository.loadSkillRating(uuid) : null;

        if (metricsCollector == null) {
            return stored != null ? stored : new SkillRating(uuid);
        }

        var metrics = metricsCollector.getMetrics(uuid);
        if (metrics.getTotalJumps() < AdaptiveConfig.getMinSessions()) {
            if (stored != null && stored.isReliable()) {
                return stored;
            }
            return stored != null ? stored : new SkillRating(uuid);
        }

        SkillRating session = AdaptiveDifficulty.analyzeSession(player, metricsCollector);
        if (stored == null) {
            return session;
        }
        return AdaptiveDifficulty.mergeSessionRating(stored, session);
    }

    @NotNull
    private static StatsRepository createRepository() {
        if (Option.SQL && StorageSQL.isConnected()) {
            return new SQLStatsStorage(StorageSQL.getConnectionManager());
        }
        return new FileStatsStorage();
    }

    private static void startCollector(@NotNull StatsRepository repo) {
        if (plugin == null) {
            return;
        }
        repository = repo;
        metricsCollector = new MetricsCollector(plugin, repo);
        metricsCollector.startAutoSave();
    }

    private static void shutdownCollector() {
        if (metricsCollector != null) {
            metricsCollector.stopAutoSave();
            metricsCollector.saveAllMetrics();
            metricsCollector = null;
        }
    }
}
