package dev.loki.loparkour.mode.elytra;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates rings for Elytra mode parkour.
 */
public class ElytraRingGenerator {
    
    private final ElytraConfig config;
    private int nextRingId = 0;
    
    public ElytraRingGenerator(@NotNull ElytraConfig config) {
        this.config = config;
    }
    
    /**
     * Generate a series of rings starting from origin in the given direction.
     */
    @NotNull
    public List<ElytraRing> generateRings(@NotNull Location origin, @NotNull Vector direction, int count) {
        List<ElytraRing> rings = new ArrayList<>();
        
        Location currentPos = origin.clone();
        Vector currentDir = direction.clone().normalize();
        
        for (int i = 0; i < count; i++) {
            // Calculate distance to next ring
            double distance = calculateRingDistance();
            
            // Move to next ring position
            currentPos.add(currentDir.clone().multiply(distance));
            
            // Apply course variation
            currentDir = applyDirectionVariation(currentDir);
            
            // Ensure we don't go too high or too low
            currentPos = constrainHeight(currentPos, origin);
            
            // Create ring with perpendicular normal
            Vector normal = findPerpendicularVector(currentDir);
            ElytraRing ring = new ElytraRing(nextRingId++, currentPos.clone(), normal, config.getRingSize());
            
            rings.add(ring);
        }
        
        return rings;
    }
    
    private double calculateRingDistance() {
        return ThreadLocalRandom.current().nextDouble(
            config.getRingDistanceMin(), 
            config.getRingDistanceMax()
        );
    }
    
    @NotNull
    private Vector applyDirectionVariation(@NotNull Vector currentDir) {
        double maxHorizontalChange = Math.toRadians(config.getMaxHeadingChangeHorizontal());
        double maxVerticalChange = Math.toRadians(config.getMaxHeadingChangeVertical());
        
        // Random horizontal rotation
        double yawChange = ThreadLocalRandom.current().nextDouble(-maxHorizontalChange, maxHorizontalChange);
        
        // Random vertical rotation
        double pitchChange = ThreadLocalRandom.current().nextDouble(-maxVerticalChange, maxVerticalChange);
        
        return rotateVector(currentDir, yawChange, pitchChange);
    }
    
    @NotNull
    private Location constrainHeight(@NotNull Location pos, @NotNull Location origin) {
        double maxHeight = origin.getY() + config.getMaxHeightAboveSpawn();
        double minHeight = origin.getY() - 20; // Don't go too low
        
        if (pos.getY() > maxHeight) {
            pos.setY(maxHeight);
        } else if (pos.getY() < minHeight) {
            pos.setY(minHeight);
        }
        
        return pos;
    }
    
    @NotNull
    private Vector findPerpendicularVector(@NotNull Vector v) {
        // Find a vector perpendicular to v
        Vector perp;
        if (Math.abs(v.getX()) < 0.9) {
            perp = new Vector(1, 0, 0);
        } else {
            perp = new Vector(0, 1, 0);
        }
        
        // Make it truly perpendicular using cross product
        return v.getCrossProduct(perp).normalize();
    }
    
    @NotNull
    private Vector rotateVector(@NotNull Vector v, double yawRad, double pitchRad) {
        // Rotate around Y axis (yaw)
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        
        double newX = v.getX() * cosYaw - v.getZ() * sinYaw;
        double newZ = v.getX() * sinYaw + v.getZ() * cosYaw;
        
        Vector rotated = new Vector(newX, v.getY(), newZ);
        
        // Rotate around perpendicular axis (pitch)
        Vector axis = findPerpendicularVector(rotated);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        
        // Rodrigues' rotation formula
        Vector result = rotated.clone().multiply(cosPitch);
        result.add(axis.getCrossProduct(rotated).multiply(sinPitch));
        result.add(axis.clone().multiply(axis.dot(rotated) * (1 - cosPitch)));
        
        return result.normalize();
    }
}