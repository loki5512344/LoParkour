package dev.loki.loparkour.session.core;
import dev.loki.loparkour.session.manager.SessionStateManager;
import dev.loki.loparkour.session.manager.SessionPlayerManager;

import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.player.spectator.ParkourSpectator;
import dev.loki.loparkour.player.core.ParkourUser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Refactored Session using composition pattern.
 * Coordinates player management and state management.
 */
public class Session {
    
    public ParkourGenerator generator;
    
    private final SessionPlayerManager playerManager;
    private final SessionStateManager stateManager;
    
    private Session() {
        this.playerManager = new SessionPlayerManager(this);
        this.stateManager = new SessionStateManager(this);
    }
    
    /**
     * Create a new session with specified parameters.
     */
    @NotNull
    public static Session create(
            @NotNull Function<Session, ParkourGenerator> generatorFunction,
            Function<Session, Boolean> isAcceptingPlayers,
            Function<Session, Boolean> isAcceptingSpectators,
            Player... players) {
        
        Session session = new Session();
        
        // Set acceptance functions
        if (isAcceptingPlayers != null) {
            session.playerManager.setAcceptingPlayers(isAcceptingPlayers);
        }
        if (isAcceptingSpectators != null) {
            session.playerManager.setAcceptingSpectators(isAcceptingSpectators);
        }
        
        // Add initial players
        List<ParkourPlayer> parkourPlayers = new ArrayList<>();
        if (players != null) {
            for (Player player : players) {
                ParkourPlayer pp = ParkourUser.register(player, session);
                session.addPlayers(pp);
                parkourPlayers.add(pp);
            }
        }
        
        // Initialize session state
        session.stateManager.initialize(generatorFunction);
        
        // Update generator settings for players
        if (session.generator != null) {
            parkourPlayers.forEach(p -> p.updateGeneratorSettings(session.generator));
        }
        
        return session;
    }
    
    // Visibility management
    public void setVisibility(@NotNull Visibility visibility) {
        stateManager.setVisibility(visibility);
    }
    
    @NotNull
    public Visibility getVisibility() {
        return stateManager.getVisibility();
    }
    
    // Player management
    public void addPlayers(@NotNull ParkourPlayer... toAdd) {
        playerManager.addPlayers(toAdd);
    }
    
    public void removePlayers(@NotNull ParkourPlayer... toRemove) {
        playerManager.removePlayers(toRemove);
    }
    
    @NotNull
    public List<ParkourPlayer> getPlayers() {
        return playerManager.getPlayers();
    }
    
    // Spectator management
    public void addSpectators(@NotNull ParkourSpectator... spectators) {
        playerManager.addSpectators(spectators);
    }
    
    public void removeSpectators(@NotNull ParkourSpectator... spectators) {
        playerManager.removeSpectators(spectators);
    }
    
    @NotNull
    public List<ParkourSpectator> getSpectators() {
        return playerManager.getSpectators();
    }
    
    @NotNull
    public List<ParkourUser> getUsers() {
        return playerManager.getUsers();
    }
    
    // State management
    public Location getSpawnLocation() {
        return stateManager.getSpawnLocation();
    }
    
    public void toggleMute(@NotNull ParkourUser user) {
        stateManager.toggleMute(user);
    }
    
    public boolean isMuted(@NotNull ParkourUser user) {
        return stateManager.isMuted(user);
    }
    
    // Acceptance checks
    public boolean isAcceptingPlayers() {
        return playerManager.isAcceptingPlayers();
    }
    
    public boolean isAcceptingSpectators() {
        return playerManager.isAcceptingSpectators();
    }
    
    // Lifecycle
    public void onAllPlayersLeft() {
        stateManager.cleanup();
    }
    
    public void setGenerator(@NotNull ParkourGenerator generator) {
        this.generator = generator;
    }
    
    // Enums
    public enum Visibility {
        PRIVATE, ID_ONLY, PUBLIC
    }
    
    public enum ChatType {
        LOBBY_ONLY, PLAYERS_ONLY, PUBLIC
    }
}
