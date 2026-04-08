package dev.loki.loparkour.session;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.world.Divider;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages session state and lifecycle.
 */
public class SessionStateManager {
    
    private final Session session;
    private Location spawnLocation;
    private Session.Visibility visibility = Session.Visibility.PUBLIC;
    private final Map<ParkourUser, Boolean> mutedUsers = new HashMap<>();
    
    public SessionStateManager(@NotNull Session session) {
        this.session = session;
    }
    
    /**
     * Initialize session with generator and spawn location.
     */
    public void initialize(@NotNull Function<Session, ParkourGenerator> generatorFunction) {
        // Allocate spawn location
        this.spawnLocation = Divider.add(session);
        
        // Create generator
        ParkourGenerator generator = generatorFunction.apply(session);
        session.setGenerator(generator);
        
        // Build island at spawn location
        generator.island.build(spawnLocation);
    }
    
    /**
     * Clean up session resources.
     */
    public void cleanup() {
        if (session.generator != null) {
            session.generator.reset(false);
        }
        
        Divider.remove(session);
        mutedUsers.clear();
    }
    
    /**
     * Set session visibility.
     */
    public void setVisibility(@NotNull Session.Visibility visibility) {
        this.visibility = visibility;
    }
    
    /**
     * Get session visibility.
     */
    @NotNull
    public Session.Visibility getVisibility() {
        return visibility;
    }
    
    /**
     * Get spawn location.
     */
    @Nullable
    public Location getSpawnLocation() {
        return spawnLocation != null ? spawnLocation.clone() : null;
    }
    
    /**
     * Toggle mute status for user.
     */
    public void toggleMute(@NotNull ParkourUser user) {
        mutedUsers.put(user, !isMuted(user));
    }
    
    /**
     * Check if user is muted.
     */
    public boolean isMuted(@NotNull ParkourUser user) {
        return mutedUsers.getOrDefault(user, false);
    }
    
    /**
     * Get muted users count.
     */
    public int getMutedUsersCount() {
        return (int) mutedUsers.values().stream().filter(muted -> muted).count();
    }
    
    /**
     * Clear all muted users.
     */
    public void clearMutedUsers() {
        mutedUsers.clear();
    }
}