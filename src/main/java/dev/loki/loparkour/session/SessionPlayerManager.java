package dev.loki.loparkour.session;

import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.player.ParkourUser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Manages players and spectators in a session.
 */
public class SessionPlayerManager {
    
    private final Session session;
    private final List<ParkourPlayer> players = new ArrayList<>();
    private final List<ParkourSpectator> spectators = new ArrayList<>();
    
    private Function<Session, Boolean> isAcceptingPlayers = s -> false;
    private Function<Session, Boolean> isAcceptingSpectators = s -> s.getVisibility() == Session.Visibility.PUBLIC;
    
    public SessionPlayerManager(@NotNull Session session) {
        this.session = session;
    }
    
    /**
     * Set player acceptance function.
     */
    public void setAcceptingPlayers(@NotNull Function<Session, Boolean> function) {
        this.isAcceptingPlayers = function;
    }
    
    /**
     * Set spectator acceptance function.
     */
    public void setAcceptingSpectators(@NotNull Function<Session, Boolean> function) {
        this.isAcceptingSpectators = function;
    }
    
    /**
     * Check if session is accepting players.
     */
    public boolean isAcceptingPlayers() {
        return isAcceptingPlayers.apply(session);
    }
    
    /**
     * Check if session is accepting spectators.
     */
    public boolean isAcceptingSpectators() {
        return isAcceptingSpectators.apply(session);
    }
    
    /**
     * Add players to session.
     */
    public void addPlayers(@NotNull ParkourPlayer... toAdd) {
        players.addAll(Arrays.asList(toAdd));
        updateVisibility();
    }
    
    /**
     * Remove players from session.
     */
    public void removePlayers(@NotNull ParkourPlayer... toRemove) {
        players.removeAll(Arrays.asList(toRemove));
        
        if (players.isEmpty()) {
            session.onAllPlayersLeft();
        } else {
            updateVisibility();
        }
    }
    
    /**
     * Add spectators to session.
     */
    public void addSpectators(@NotNull ParkourSpectator... toAdd) {
        spectators.addAll(Arrays.asList(toAdd));
        updateVisibility();
    }
    
    /**
     * Remove spectators from session.
     */
    public void removeSpectators(@NotNull ParkourSpectator... toRemove) {
        spectators.removeAll(Arrays.asList(toRemove));
        updateVisibility();
    }
    
    /**
     * Get all players.
     */
    @NotNull
    public List<ParkourPlayer> getPlayers() {
        return new ArrayList<>(players);
    }
    
    /**
     * Get all spectators.
     */
    @NotNull
    public List<ParkourSpectator> getSpectators() {
        return new ArrayList<>(spectators);
    }
    
    /**
     * Get all users (players + spectators).
     */
    @NotNull
    public List<ParkourUser> getUsers() {
        List<ParkourUser> users = new ArrayList<>();
        users.addAll(players);
        users.addAll(spectators);
        return users;
    }
    
    /**
     * Get total user count.
     */
    public int getTotalUsers() {
        return players.size() + spectators.size();
    }
    
    /**
     * Check if session has any users.
     */
    public boolean hasUsers() {
        return !players.isEmpty() || !spectators.isEmpty();
    }
    
    private void updateVisibility() {
        // Update player visibility for all users
        List<ParkourUser> allUsers = getUsers();
        
        for (ParkourUser user : allUsers) {
            for (ParkourUser other : allUsers) {
                if (!user.equals(other)) {
                    user.player.showPlayer(dev.loki.loparkour.LoParkour.getPlugin(), other.player);
                }
            }
        }
    }
}