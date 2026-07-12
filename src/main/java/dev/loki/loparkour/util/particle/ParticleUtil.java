package dev.loki.loparkour.util.particle;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/**
 * Utility class for particle effects
 * Replaces vilib Particles
 */
public class ParticleUtil {

    private ParticleUtil() {
    }

    /**
     * Shows a box outline with particles
     *
     * @param box      The bounding box
     * @param world    The world
     * @param particle The particle type
     * @param player   The player to show particles to
     * @param spacing  The spacing between particles
     */
    public static void box(BoundingBox box, World world, Particle particle, Player player, double spacing) {
        double minX = box.getMinX();
        double minY = box.getMinY();
        double minZ = box.getMinZ();
        double maxX = box.getMaxX();
        double maxY = box.getMaxY();
        double maxZ = box.getMaxZ();

        // Draw edges
        for (double x = minX; x <= maxX; x += spacing) {
            player.spawnParticle(particle, x, minY, minZ, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, x, minY, maxZ, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, x, maxY, minZ, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, x, maxY, maxZ, 1, 0, 0, 0, 0);
        }

        for (double y = minY; y <= maxY; y += spacing) {
            player.spawnParticle(particle, minX, y, minZ, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, minX, y, maxZ, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, maxX, y, minZ, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, maxX, y, maxZ, 1, 0, 0, 0, 0);
        }

        for (double z = minZ; z <= maxZ; z += spacing) {
            player.spawnParticle(particle, minX, minY, z, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, minX, maxY, z, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, maxX, minY, z, 1, 0, 0, 0, 0);
            player.spawnParticle(particle, maxX, maxY, z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Shows particles at a location
     *
     * @param location The location
     * @param particle The particle type
     * @param count    The number of particles
     */
    public static void show(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(particle, location, count, 0, 0, 0, 0);
    }

    /**
     * Draws particles at a location with offset
     *
     * @param location Location
     * @param particle Particle type
     * @param count    Number of particles
     * @param offsetX  X offset
     * @param offsetY  Y offset
     * @param offsetZ  Z offset
     * @param speed    Particle speed
     */
    public static void draw(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        if (location.getWorld() != null) {
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    /**
     * Draws particles in a circle
     *
     * @param center   Center location
     * @param particle Particle type
     * @param radius   Circle radius
     * @param points   Number of points
     */
    public static void circle(Location center, Particle particle, int radius, int points) {
        if (center.getWorld() == null) {
            return;
        }

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            center.getWorld().spawnParticle(particle, x, center.getY(), z, 1, 0, 0, 0, 0);
        }
    }
}
