package dev.loki.loparkour.util.world;

import org.bukkit.Location;

/**
 * Location utilities (replacement for vilib Locations)
 */
public class Locations {

    /**
     * Gets the maximum location from two locations (highest x, y, z)
     *
     * @param a First location
     * @param b Second location
     * @return Location with maximum coordinates
     */
    public static Location max(Location a, Location b) {
        return new Location(
            a.getWorld(),
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );
    }

    /**
     * Gets the minimum location from two locations (lowest x, y, z)
     *
     * @param a First location
     * @param b Second location
     * @return Location with minimum coordinates
     */
    public static Location min(Location a, Location b) {
        return new Location(
            a.getWorld(),
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );
    }

    /**
     * Converts location to string
     *
     * @param location Location to convert
     * @param includeYawPitch Whether to include yaw and pitch
     * @return String representation
     */
    public static String toString(Location location, boolean includeYawPitch) {
        if (includeYawPitch) {
            return String.format("%.2f, %.2f, %.2f (yaw: %.2f, pitch: %.2f)",
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        }
        return String.format("%.2f, %.2f, %.2f",
            location.getX(), location.getY(), location.getZ());
    }
}
