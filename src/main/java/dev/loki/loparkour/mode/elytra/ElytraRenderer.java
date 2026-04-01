package dev.loki.loparkour.mode.elytra;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles visual rendering of Elytra rings and effects.
 */
public class ElytraRenderer {
    
    private static final int RING_PARTICLES = 32;
    private static final Color ACTIVE_RING_COLOR = Color.LIME;
    private static final Color PASSED_RING_COLOR = Color.GRAY;
    private static final Color UPCOMING_RING_COLOR = Color.YELLOW;
    
    /**
     * Render all rings with appropriate colors.
     */
    public void renderRings(@NotNull List<ElytraRing> rings, int currentRingIndex) {
        for (int i = 0; i < rings.size(); i++) {
            ElytraRing ring = rings.get(i);
            Color color = getRingColor(i, currentRingIndex, ring.isPassed());
            renderRing(ring, color);
        }
    }
    
    /**
     * Render a single ring with the specified color.
     */
    public void renderRing(@NotNull ElytraRing ring, @NotNull Color color) {
        Location center = ring.getCenter();
        Vector normal = ring.getNormal();
        double radius = ring.getRadius();
        
        // Create two perpendicular vectors to the normal
        Vector u = findPerpendicular(normal);
        Vector v = normal.getCrossProduct(u).normalize();
        
        // Draw circle using particles
        World world = center.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < RING_PARTICLES; i++) {
            double angle = 2 * Math.PI * i / RING_PARTICLES;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            
            Vector offset = u.clone().multiply(x).add(v.clone().multiply(y));
            Location particlePos = center.clone().add(offset);
            
            // Spawn colored particle
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
            world.spawnParticle(Particle.REDSTONE, particlePos, 1, dustOptions);
        }
        
        // Create or update armor stand for ring center (optional visual aid)
        if (ring.getEntity() == null) {
            ArmorStand stand = (ArmorStand) world.spawnEntity(center, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setCustomNameVisible(true);
            ring.setEntity(stand);
        }
        
        // Update armor stand name with ring info
        ArmorStand stand = ring.getEntity();
        if (stand != null && !stand.isDead()) {
            stand.setCustomName("Ring #" + ring.getId());
        }
    }
    
    @NotNull
    private Color getRingColor(int ringIndex, int currentRingIndex, boolean passed) {
        if (passed) {
            return PASSED_RING_COLOR;
        } else if (ringIndex == currentRingIndex) {
            return ACTIVE_RING_COLOR;
        } else {
            return UPCOMING_RING_COLOR;
        }
    }
    
    @NotNull
    private Vector findPerpendicular(@NotNull Vector v) {
        // Find a vector perpendicular to v
        if (Math.abs(v.getX()) < 0.9) {
            return new Vector(1, 0, 0).getCrossProduct(v).normalize();
        } else {
            return new Vector(0, 1, 0).getCrossProduct(v).normalize();
        }
    }
}