package dev.loki.loparkour.mode.elytra;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a ring that players must fly through in Elytra mode
 */
public class ElytraRing {

    private final Location center;
    private final int radius;
    private final Vector direction;
    private final int index;

    public ElytraRing(@NotNull Location center, int radius, @NotNull Vector direction, int index) {
        this.center = center.clone();
        this.radius = radius;
        this.direction = direction.clone().normalize();
        this.index = index;
    }

    @NotNull
    public Location getCenter() {
        return center.clone();
    }

    public int getRadius() {
        return radius;
    }

    @NotNull
    public Vector getDirection() {
        return direction.clone();
    }

    public int getIndex() {
        return index;
    }

    /**
     * Check if a location is inside this ring
     */
    public boolean contains(@NotNull Location location) {
        if (!location.getWorld().equals(center.getWorld())) {
            return false;
        }

        Vector toLocation = location.toVector().subtract(center.toVector());
        double distanceFromCenter = toLocation.length();

        return distanceFromCenter <= radius;
    }

    /**
     * Check if a location is centered (within inner radius) in this ring
     */
    public boolean isCentered(@NotNull Location location, double innerRadiusPercent) {
        if (!location.getWorld().equals(center.getWorld())) {
            return false;
        }

        Vector toLocation = location.toVector().subtract(center.toVector());
        double distanceFromCenter = toLocation.length();
        double innerRadius = radius * innerRadiusPercent;

        return distanceFromCenter <= innerRadius;
    }

    /**
     * Remove visual representation (called on cleanup)
     */
    public void remove() {
        // Particles are temporary, nothing to clean up
    }
}
