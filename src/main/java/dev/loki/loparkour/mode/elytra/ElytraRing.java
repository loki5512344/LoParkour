package dev.loki.loparkour.mode.elytra;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a ring that players must fly through in Elytra mode.
 */
public class ElytraRing {
    
    private final int id;
    private final Location center;
    private final Vector normal;
    private final double radius;
    private ArmorStand entity;
    private boolean passed = false;
    
    public ElytraRing(int id, @NotNull Location center, @NotNull Vector normal, double radius) {
        this.id = id;
        this.center = center.clone();
        this.normal = normal.clone().normalize();
        this.radius = radius;
    }
    
    /**
     * Check if player has crossed through this ring.
     */
    public boolean hasCrossed(@NotNull Location playerLoc, @NotNull Vector velocity) {
        if (passed) return false;
        
        Vector toPlayer = playerLoc.toVector().subtract(center.toVector());
        double distanceToPlane = toPlayer.dot(normal);
        
        // Check if player is close to the ring plane
        if (Math.abs(distanceToPlane) > 2.0) return false;
        
        // Check if moving towards the ring
        if (velocity.dot(normal) <= 0) return false;
        
        // Check if within ring radius
        Vector projectedPos = toPlayer.subtract(normal.clone().multiply(distanceToPlane));
        return projectedPos.length() <= radius;
    }
    
    /**
     * Check if player is currently inside the ring.
     */
    public boolean isInside(@NotNull Location playerLoc) {
        Vector toPlayer = playerLoc.toVector().subtract(center.toVector());
        double distanceToPlane = Math.abs(toPlayer.dot(normal));
        
        if (distanceToPlane > 1.0) return false;
        
        Vector projectedPos = toPlayer.subtract(normal.clone().multiply(toPlayer.dot(normal)));
        return projectedPos.length() <= radius;
    }
    
    /**
     * Remove the ring's visual representation.
     */
    public void remove() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }
    
    // Getters
    public int getId() { return id; }
    public Location getCenter() { return center.clone(); }
    public Vector getNormal() { return normal.clone(); }
    public double getRadius() { return radius; }
    public ArmorStand getEntity() { return entity; }
    public boolean isPassed() { return passed; }
    
    // Setters
    public void setEntity(ArmorStand entity) { this.entity = entity; }
    public void setPassed(boolean passed) { this.passed = passed; }
}