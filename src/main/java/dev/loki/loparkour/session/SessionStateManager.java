package dev.loki.loparkour.session;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.world.Divider;
import dev.lolib.scheduler.Scheduler;
import dev.lolib.scheduler.ScheduledTask;
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
    private ScheduledTask tickTask = null;
    
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

        // Start tick cycle for generator
        startTickCycle();
    }
    
    /**
     * Clean up session resources.
     */
    public void cleanup() {
        // Stop tick cycle
        stopTickCycle();

        if (session.generator != null) {
            session.generator.reset(false);
        }

        Divider.remove(session);
        mutedUsers.clear();
    }

    /**
     * Start tick cycle for generator.
     */
    private void startTickCycle() {
        if (session.generator == null || tickTask != null) {
            return;
        }

        tickTask = Scheduler.get(LoParkour.getPlugin()).runTimer(() -> {
            if (session.generator != null) {
                session.generator.tick();
            }
        }, 0, 1); // Run every tick (1 = 50ms)
    }

    /**
     * Stop tick cycle.
     */
    private void stopTickCycle() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
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