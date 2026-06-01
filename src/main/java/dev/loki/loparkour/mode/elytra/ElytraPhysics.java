package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.player.core.ParkourPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles physics checks for Elytra mode (ring crossings, falls, boosts)
 */
public class ElytraPhysics {

    private final ElytraConfig config;
    private final Map<UUID, Long> lastFireworkBoost = new HashMap<>();

    public ElytraPhysics(@NotNull ElytraConfig config) {
        this.config = config;
    }

    /**
     * Check if player has crossed any new rings
     * @return new ring index if crossed, otherwise current index
     */
    public int checkRingCrossings(@NotNull ParkourPlayer player, @NotNull List<ElytraRing> rings, int currentIndex) {
        if (currentIndex >= rings.size()) {
            return currentIndex;
        }

        Location playerLoc = player.player.getLocation();
        
        // Check next ring
        ElytraRing nextRing = rings.get(currentIndex);
        if (nextRing.contains(playerLoc)) {
            return currentIndex + 1;
        }

        return currentIndex;
    }

    /**
     * Check if player should fall (too far from course)
     */
    @NotNull
    public FallCheckResult checkFall(@NotNull ParkourPlayer player, @NotNull List<ElytraRing> rings, int currentIndex) {
        if (rings.isEmpty()) {
            return FallCheckResult.noFall();
        }

        Location playerLoc = player.player.getLocation();

        // Check if player is too far from nearest ring
        ElytraRing nearestRing = currentIndex < rings.size() ? rings.get(currentIndex) : rings.get(rings.size() - 1);
        double distance = playerLoc.distance(nearestRing.getCenter());

        if (distance > config.getMaxDeviation()) {
            return FallCheckResult.fall("Too far from course");
        }

        // Check if player is gliding
        if (!player.player.isGliding()) {
            return FallCheckResult.fall("Not gliding");
        }

        return FallCheckResult.noFall();
    }

    /**
     * Check if player passed through center of ring (for bonus points)
     */
    public boolean isPlayerCentered(@NotNull Location playerLoc, @NotNull ElytraRing ring) {
        return ring.isCentered(playerLoc, 0.5); // 50% of radius
    }

    /**
     * Apply firework boost if cooldown allows
     */
    public boolean applyFireworkBoost(@NotNull ParkourPlayer player) {
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        Long lastBoost = lastFireworkBoost.get(uuid);

        if (lastBoost != null && (now - lastBoost) < config.getFireworkCooldownMs()) {
            return false; // Still on cooldown
        }

        // Apply boost
        Player p = player.player;
        Vector velocity = p.getVelocity();
        Vector boost = p.getLocation().getDirection().multiply(config.getBoostPower());
        p.setVelocity(velocity.add(boost));

        lastFireworkBoost.put(uuid, now);
        return true;
    }

    /**
     * Cleanup player data
     */
    public void cleanup(@NotNull UUID uuid) {
        lastFireworkBoost.remove(uuid);
    }

    /**
     * Result of fall check
     */
    public static class FallCheckResult {
        private final boolean shouldFall;
        private final String reason;

        private FallCheckResult(boolean shouldFall, String reason) {
            this.shouldFall = shouldFall;
            this.reason = reason;
        }

        public static FallCheckResult fall(String reason) {
            return new FallCheckResult(true, reason);
        }

        public static FallCheckResult noFall() {
            return new FallCheckResult(false, "");
        }

        public boolean shouldFall() {
            return shouldFall;
        }

        public String getReason() {
            return reason;
        }
    }
}
