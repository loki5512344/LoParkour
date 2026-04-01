package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.player.ParkourPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles physics calculations and validation for Elytra mode.
 */
public class ElytraPhysics {
    
    private final ElytraConfig config;
    private final Map<UUID, Long> lastFireworkTime = new HashMap<>();
    
    public ElytraPhysics(@NotNull ElytraConfig config) {
        this.config = config;
    }
    
    /**
     * Check if any rings have been crossed by the player.
     */
    public int checkRingCrossings(@NotNull ParkourPlayer player, @NotNull List<ElytraRing> rings, int currentRingIndex) {
        Player p = player.player;
        Location playerLoc = p.getLocation();
        Vector velocity = p.getVelocity();
        
        // Check rings starting from current index
        for (int i = currentRingIndex; i < rings.size(); i++) {
            ElytraRing ring = rings.get(i);
            
            if (ring.hasCrossed(playerLoc, velocity)) {
                ring.setPassed(true);
                return i + 1; // Return new current ring index
            }
            
            // Only check a few rings ahead to avoid skipping
            if (i > currentRingIndex + 2) break;
        }
        
        return currentRingIndex; // No change
    }
    
    /**
     * Check if player should fall due to deviation from course.
     */
    @NotNull
    public FallCheckResult checkFall(@NotNull ParkourPlayer player, @NotNull List<ElytraRing> rings, int currentRingIndex) {
        Player p = player.player;
        Location playerLoc = p.getLocation();
        
        if (rings.isEmpty() || currentRingIndex >= rings.size()) {
            return new FallCheckResult(false, "No rings available");
        }
        
        // Check distance to current ring
        ElytraRing currentRing = rings.get(currentRingIndex);
        double distanceToRing = playerLoc.distance(currentRing.getCenter());
        
        if (distanceToRing > config.getMaxDeviation()) {
            return new FallCheckResult(true, "Too far from course (distance: " + String.format("%.1f", distanceToRing) + ")");
        }
        
        // Check if player is flying (has elytra deployed)
        if (!p.isGliding()) {
            return new FallCheckResult(true, "Not gliding");
        }
        
        return new FallCheckResult(false, null);
    }
    
    /**
     * Apply firework boost to player if cooldown allows.
     */
    public boolean applyFireworkBoost(@NotNull ParkourPlayer player) {
        Player p = player.player;
        UUID playerId = p.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        Long lastBoost = lastFireworkTime.get(playerId);
        if (lastBoost != null && (currentTime - lastBoost) < config.getFireworkCooldownMs()) {
            return false; // Still on cooldown
        }
        
        // Apply boost
        Vector velocity = p.getVelocity();
        Vector boostedVelocity = velocity.multiply(config.getBoostPower());
        p.setVelocity(boostedVelocity);
        
        // Update cooldown
        lastFireworkTime.put(playerId, currentTime);
        
        return true;
    }
    
    /**
     * Check if player is centered when passing through a ring.
     */
    public boolean isPlayerCentered(@NotNull Location playerLoc, @NotNull ElytraRing ring) {
        Vector toPlayer = playerLoc.toVector().subtract(ring.getCenter().toVector());
        Vector normal = ring.getNormal();
        
        // Project player position onto ring plane
        Vector projectedPos = toPlayer.subtract(normal.clone().multiply(toPlayer.dot(normal)));
        
        // Check if within center threshold (25% of ring radius)
        double centerThreshold = ring.getRadius() * 0.25;
        return projectedPos.length() <= centerThreshold;
    }
    
    /**
     * Clean up physics data for a player.
     */
    public void cleanup(@NotNull UUID playerId) {
        lastFireworkTime.remove(playerId);
    }
    
    /**
     * Result of fall check.
     */
    public static class FallCheckResult {
        private final boolean shouldFall;
        private final String reason;
        
        public FallCheckResult(boolean shouldFall, String reason) {
            this.shouldFall = shouldFall;
            this.reason = reason;
        }
        
        public boolean shouldFall() { return shouldFall; }
        public String getReason() { return reason; }
    }
}