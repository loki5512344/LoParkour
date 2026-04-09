package dev.loki.loparkour.mode.elytra;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders rings using particles for Elytra mode
 */
public class ElytraRenderer {

    private static final int PARTICLES_PER_RING = 32;

    /**
     * Render rings starting from currentIndex
     */
    public void renderRings(@NotNull List<ElytraRing> rings, int currentIndex) {
        // Render next few rings ahead of player
        int renderCount = Math.min(5, rings.size() - currentIndex);
        
        for (int i = 0; i < renderCount; i++) {
            int index = currentIndex + i;
            if (index >= rings.size()) break;
            
            ElytraRing ring = rings.get(index);
            renderRing(ring, i == 0); // Highlight next ring
        }
    }

    private void renderRing(@NotNull ElytraRing ring, boolean isNext) {
        Location center = ring.getCenter();
        int radius = ring.getRadius();
        Vector direction = ring.getDirection();

        // Calculate perpendicular vectors for ring plane
        Vector up = new Vector(0, 1, 0);
        Vector right = direction.clone().crossProduct(up).normalize();
        Vector actualUp = right.clone().crossProduct(direction).normalize();

        // Particle color based on if it's the next ring
        Particle particle = isNext ? Particle.VILLAGER_HAPPY : Particle.END_ROD;

        // Draw ring circle
        for (int i = 0; i < PARTICLES_PER_RING; i++) {
            double angle = 2 * Math.PI * i / PARTICLES_PER_RING;
            double x = Math.cos(angle) * radius;
            double y = Math.sin(angle) * radius;

            Vector offset = right.clone().multiply(x).add(actualUp.clone().multiply(y));
            Location particleLoc = center.clone().add(offset);

            center.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}
