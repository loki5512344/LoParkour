package dev.loki.loparkour.mode.elytra;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates rings along a curved aerial path for Elytra mode
 */
public class ElytraRingGenerator {

    private final ElytraConfig config;
    private final Random random = new Random();
    private int ringCounter = 0;

    public ElytraRingGenerator(@NotNull ElytraConfig config) {
        this.config = config;
    }

    /**
     * Generate multiple rings starting from a location in a direction
     */
    @NotNull
    public List<ElytraRing> generateRings(@NotNull Location start, @NotNull Vector direction, int count) {
        List<ElytraRing> rings = new ArrayList<>();
        Location current = start.clone();
        Vector currentDir = direction.clone().normalize();

        for (int i = 0; i < count; i++) {
            // Random distance between min and max
            int distance = config.getRingDistanceMin() + 
                random.nextInt(config.getRingDistanceMax() - config.getRingDistanceMin() + 1);

            // Apply random heading changes
            currentDir = applyHeadingChange(currentDir);

            // Move to next ring position
            current.add(currentDir.clone().multiply(distance));

            // Clamp height
            double spawnY = start.getY();
            double maxY = spawnY + config.getMaxHeightAboveSpawn();
            if (current.getY() > maxY) {
                current.setY(maxY);
            }
            if (current.getY() < spawnY - 20) {
                current.setY(spawnY - 20);
            }

            ElytraRing ring = new ElytraRing(current, config.getRingSize(), currentDir, ringCounter++);
            rings.add(ring);
        }

        return rings;
    }

    @NotNull
    private Vector applyHeadingChange(@NotNull Vector direction) {
        // Random horizontal angle change
        double horizontalChange = (random.nextDouble() * 2 - 1) * config.getMaxHeadingChangeHorizontal();
        
        // Random vertical angle change
        double verticalChange = (random.nextDouble() * 2 - 1) * config.getMaxHeadingChangeVertical();

        // Apply rotation
        Vector result = direction.clone();
        
        // Horizontal rotation (yaw)
        double yaw = Math.toRadians(horizontalChange);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = result.getX() * cos - result.getZ() * sin;
        double z = result.getX() * sin + result.getZ() * cos;
        result.setX(x);
        result.setZ(z);

        // Vertical rotation (pitch)
        double pitch = Math.toRadians(verticalChange);
        result.setY(result.getY() + Math.sin(pitch) * 0.5);

        return result.normalize();
    }
}
