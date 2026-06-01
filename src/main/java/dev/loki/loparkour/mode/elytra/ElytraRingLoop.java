package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.player.core.ParkourPlayer;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Per-tick ring crossing checks and ahead-of-player ring generation.
 */
public final class ElytraRingLoop {

    private final ElytraConfig config;
    private final ElytraRingGenerator ringGenerator;
    private final ElytraPhysics physics;
    private final List<ElytraRing> rings;
    private final Map<ParkourPlayer, Integer> playerRingIndex;
    private final Consumer<ParkourPlayer> onFall;
    private final RingPassHandler onRingPassed;

    @FunctionalInterface
    public interface RingPassHandler {
        void onPassed(@NotNull ParkourPlayer player, @NotNull ElytraRing ring);
    }

    public ElytraRingLoop(
            @NotNull ElytraConfig config,
            @NotNull ElytraRingGenerator ringGenerator,
            @NotNull ElytraPhysics physics,
            @NotNull List<ElytraRing> rings,
            @NotNull Map<ParkourPlayer, Integer> playerRingIndex,
            @NotNull RingPassHandler onRingPassed,
            @NotNull Consumer<ParkourPlayer> onFall) {
        this.config = config;
        this.ringGenerator = ringGenerator;
        this.physics = physics;
        this.rings = rings;
        this.playerRingIndex = playerRingIndex;
        this.onRingPassed = onRingPassed;
        this.onFall = onFall;
    }

    public void tickPlayers(@NotNull Iterable<ParkourPlayer> players, @NotNull ParkourGenerator generator) {
        for (ParkourPlayer player : players) {
            int currentIndex = playerRingIndex.getOrDefault(player, 0);

            int newIndex = physics.checkRingCrossings(player, rings, currentIndex);
            if (newIndex > currentIndex) {
                onRingPassed.onPassed(player, rings.get(newIndex - 1));
                playerRingIndex.put(player, newIndex);
            }

            ElytraPhysics.FallCheckResult fallResult = physics.checkFall(player, rings, currentIndex);
            if (fallResult.shouldFall()) {
                ElytraScoring.notifyFall(player, fallResult.getReason());
                onFall.accept(player);
            }

            int displayIndex = playerRingIndex.getOrDefault(player, 0);
            ElytraHud.sendActionBar(player, displayIndex, rings.size(), generator.state.score);
        }
    }

    public void ensureRingBuffer() {
        if (rings.isEmpty()) {
            return;
        }
        if (rings.size() < config.getRingLead() * 2) {
            ElytraRing lastRing = rings.get(rings.size() - 1);
            Vector direction = new Vector(1, 0, 0);
            rings.addAll(ringGenerator.generateRings(lastRing.getCenter(), direction, config.getRingLead()));
        }
    }

    public static void spawnInitialRings(
            @NotNull Location spawn,
            @NotNull ElytraConfig config,
            @NotNull ElytraRingGenerator ringGenerator,
            @NotNull List<ElytraRing> rings) {
        rings.addAll(ringGenerator.generateRings(spawn, new Vector(1, 0, 0), config.getRingLead()));
    }
}
