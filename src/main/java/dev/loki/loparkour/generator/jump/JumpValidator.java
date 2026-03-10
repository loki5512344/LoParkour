package dev.loki.loparkour.generator.jump;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class JumpValidator {

    private static final double MAX_JUMP_DISTANCE = 4.5;
    private static final double MAX_HORIZONTAL_DISTANCE = 4.1;
    private static final double MAX_VERTICAL_UP = 1.25;
    private static final double MAX_VERTICAL_DOWN = 3.0;

    private final double maxDistance;
    private final double maxHorizontal;
    private final double maxVerticalUp;
    private final double maxVerticalDown;

    public JumpValidator() {
        this(MAX_JUMP_DISTANCE, MAX_HORIZONTAL_DISTANCE, MAX_VERTICAL_UP, MAX_VERTICAL_DOWN);
    }

    public JumpValidator(double maxDistance, double maxHorizontal, double maxVerticalUp, double maxVerticalDown) {
        this.maxDistance = maxDistance;
        this.maxHorizontal = maxHorizontal;
        this.maxVerticalUp = maxVerticalUp;
        this.maxVerticalDown = maxVerticalDown;
    }

    public boolean canJump(@NotNull Location from, @NotNull Location to) {
        // Null safety check
        if (from == null || to == null) {
            return false;
        }
        return canJump(from.toVector(), to.toVector());
    }

    // Check if jump is possible: sqrt(dx² + dy² + dz²) <= maxDistance
    public boolean canJump(@NotNull Vector from, @NotNull Vector to) {
        // Null safety check
        if (from == null || to == null) {
            return false;
        }
        
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        // Check if same location (no jump)
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 0.01 && Math.abs(dy) < 0.01) {
            return false; // Same location
        }

        // Check horizontal distance limit
        if (horizontalDistance > maxHorizontal) {
            return false;
        }

        // Check vertical limits
        if (dy > maxVerticalUp || dy < -maxVerticalDown) {
            return false;
        }
        
        // For upward jumps, reduce max horizontal distance
        // Player can't jump as far when jumping up
        if (dy > 0) {
            double adjustedMaxHorizontal = maxHorizontal - (dy * 0.5); // Reduce by 0.5 blocks per block up
            if (horizontalDistance > adjustedMaxHorizontal) {
                return false;
            }
        }

        // Check total distance
        double totalDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return totalDistance <= maxDistance;
    }

    public double calculateDistance(@NotNull Location from, @NotNull Location to) {
        return calculateDistance(from.toVector(), to.toVector());
    }

    public double calculateDistance(@NotNull Vector from, @NotNull Vector to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double calculateHorizontalDistance(@NotNull Vector from, @NotNull Vector to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public double getMaxHorizontal() {
        return maxHorizontal;
    }

    public double getMaxVerticalUp() {
        return maxVerticalUp;
    }

    public double getMaxVerticalDown() {
        return maxVerticalDown;
    }
}
