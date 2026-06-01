package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.mode.elytra.*;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.session.core.Session;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elytra mode generator — coordinates ring loop, rendering, and player kit.
 */
public class ElytraGenerator extends ParkourGenerator {

    private final ElytraConfig config;
    private final ElytraRingGenerator ringGenerator;
    private final ElytraPhysics physics;
    private final ElytraRenderer renderer;
    private final ElytraRingLoop ringLoop;
    private final ElytraFireworkListener fireworkListener;

    private final List<ElytraRing> rings = new ArrayList<>();
    private final Map<ParkourPlayer, Integer> playerRingIndex = new HashMap<>();

    private BukkitTask renderTask;

    public ElytraGenerator(@NotNull Session session) {
        super(session);

        this.config = new ElytraConfig();
        this.ringGenerator = new ElytraRingGenerator(config);
        this.physics = new ElytraPhysics(config);
        this.renderer = new ElytraRenderer();
        this.ringLoop = new ElytraRingLoop(
                config, ringGenerator, physics, rings, playerRingIndex,
                (player, ring) -> ElytraScoring.onRingPassed(this, player, ring, physics),
                p -> fall());
        this.fireworkListener = new ElytraFireworkListener(
                this::findParkourPlayer,
                (pp, p) -> {
                    if (physics.applyFireworkBoost(pp)) {
                        ElytraPlayerKit.spawnBoostFirework(p);
                        return true;
                    }
                    return false;
                });

        Bukkit.getPluginManager().registerEvents(fireworkListener, LoParkour.getPlugin());
    }

    @Override
    public void generateFirst(Location spawn, Location blockSpawn) {
        ElytraRingLoop.spawnInitialRings(spawn, config, ringGenerator, rings);

        for (ParkourPlayer player : getPlayers()) {
            playerRingIndex.put(player, 0);
            ElytraPlayerKit.giveElytraAndFireworks(player, config);
            ElytraPlayerKit.launch(player, spawn);
        }

        startRenderTask();
    }

    @Override
    public void tick() {
        super.tick();
        ringLoop.tickPlayers(getPlayers(), this);
        ringLoop.ensureRingBuffer();
    }

    @Override
    public void reset(boolean regenerate) {
        cleanup();
        super.reset(regenerate);
    }

    private void startRenderTask() {
        renderTask = Bukkit.getScheduler().runTaskTimer(LoParkour.getPlugin(), () -> {
            for (ParkourPlayer player : getPlayers()) {
                int currentIndex = playerRingIndex.getOrDefault(player, 0);
                renderer.renderRings(rings, currentIndex);
            }
        }, 0L, 2L);
    }

    private void cleanup() {
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }

        HandlerList.unregisterAll(fireworkListener);

        rings.forEach(ElytraRing::remove);
        rings.clear();
        playerRingIndex.clear();

        for (ParkourPlayer player : getPlayers()) {
            physics.cleanup(player.player.getUniqueId());
        }
    }

    @Nullable
    private ParkourPlayer findParkourPlayer(@NotNull Player player) {
        return getPlayers().stream()
                .filter(pp -> pp.player.equals(player))
                .findFirst()
                .orElse(null);
    }
}
