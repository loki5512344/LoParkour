package dev.loki.loparkour.ghost.model;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class GhostFrame {

    private final long timestamp;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public GhostFrame(long timestamp, @NotNull Location location) {
        this.timestamp = timestamp;
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public GhostFrame(long timestamp, double x, double y, double z, float yaw, float pitch) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Location toLocation(org.bukkit.World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
