package dev.loki.loparkour.session;

import java.util.ArrayList;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.world.Divider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * <p>A session is bound to a {@link Divider} section.
 * It manages all players, all spectators, visibility, the generator, etc.</p>
 * <p>Iteration 2.</p>
 *
 * @author loki
 * @since 5.0.0
 */
public class Session {

    /**
     * The spawn location of this session.
     */
    private Location spawnLocation;

    /**
     * The generator.
     */
    public ParkourGenerator generator;

    /**
     * The visibility of this session. Default public.
     */
    private Visibility visibility = Visibility.PUBLIC;

    /**
     * Manages users (players and spectators) in this session.
     */
    private final SessionUserManager userManager = new SessionUserManager(this);

    /**
     * Function that takes the current session and returns whether new players should be accepted.
     */
    private Function<Session, Boolean> isAcceptingPlayers = session -> false;

    /**
     * Function that takes the current session and returns whether new spectators should be accepted.
     */
    private Function<Session, Boolean> isAcceptingSpectators = session -> session.visibility == Visibility.PUBLIC;

    /**
     * Creates a new session.
     *
     * @param generatorFunction     The generator function.
     * @param isAcceptingPlayers    The function that takes the current session and returns whether new players should be accepted.
     * @param isAcceptingSpectators The function that takes the current session and returns whether new spectators should be accepted.
     * @param players               The players.
     * @return The session.
     */
    public static Session create(Function<Session, ParkourGenerator> generatorFunction,
                                 Function<Session, Boolean> isAcceptingPlayers,
                                 Function<Session, Boolean> isAcceptingSpectators,
                                 Player... players) {
        Session session = new Session();

        if (isAcceptingPlayers != null) session.isAcceptingPlayers = isAcceptingPlayers;
        if (isAcceptingSpectators != null) session.isAcceptingSpectators = isAcceptingSpectators;

        List<ParkourPlayer> pps = new ArrayList<>();
        if (players != null) {
            for (Player player : players) {
                ParkourPlayer pp = ParkourUser.register(player, session);
                session.addPlayers(pp);
                pps.add(pp);
            }
        }

        session.spawnLocation = Divider.add(session);
        session.generator = generatorFunction.apply(session);

        if (players != null) {
            pps.forEach(p -> p.updateGeneratorSettings(session.generator));
        }

        session.generator.island.build(session.spawnLocation);

        return session;
    }

    /**
     * Sets the visibility of this session.
     * @param visibility The visibility.
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * @return The visibility of this session.
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Adds provided players to this session's player list.
     *
     * @param toAdd The players to add.
     */
    public void addPlayers(ParkourPlayer... toAdd) {
        userManager.addPlayers(toAdd);
    }

    /**
     * Removes provided players from this session's player list.
     *
     * @param toRemove The players to remove.
     */
    public void removePlayers(ParkourPlayer... toRemove) {
        userManager.removePlayers(toRemove);
    }

    /**
     * @return The players.
     */
    public List<ParkourPlayer> getPlayers() {
        return userManager.getPlayers();
    }

    /**
     * Adds provided spectators to this session's spectator list.
     *
     * @param spectators The spectators to add.
     */
    public void addSpectators(ParkourSpectator... spectators) {
        userManager.addSpectators(spectators);
    }

    /**
     * Removes provided spectators from this session's spectator list.
     *
     * @param spectators The spectators to remove.
     */
    public void removeSpectators(ParkourSpectator... spectators) {
        userManager.removeSpectators(spectators);
    }

    /**
     * @return The spectators.
     */
    public List<ParkourSpectator> getSpectators() {
        return userManager.getSpectators();
    }

    /**
     * @return The users.
     */
    public List<ParkourUser> getUsers() {
        return userManager.getUsers();
    }

    /**
     * @return the spawn location for this {@link Session}.
     */
    @SuppressWarnings("unused")
    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    /**
     * Toggles mute for the specified user.
     *
     * @param user The user to (un)mute.
     */
    public void toggleMute(@NotNull ParkourUser user) {
        userManager.toggleMute(user);
    }

    /**
     * @param user The user.
     * @return True when the user is muted, false if not.
     */
    public boolean isMuted(@NotNull ParkourUser user) {
        return userManager.isMuted(user);
    }

    /**
     * Called when all players have left the session.
     */
    void onAllPlayersLeft() {
        generator.reset(false);
        Divider.remove(this);
    }

    /**
     * @return True when players may join this session, false if not.
     */
    public boolean isAcceptingPlayers() {
        return isAcceptingPlayers.apply(this);
    }

    /**
     * @return True when spectators may join this session, false if not.
     */
    public boolean isAcceptingSpectators() {
        return isAcceptingSpectators.apply(this);
    }

    public enum Visibility {

        /**
         * No-one can join.
         */
        PRIVATE,

        /**
         * Only people with the session id can join.
         */
        ID_ONLY,

        /**
         * Anyone can join.
         */
        PUBLIC,

    }

    /**
     * An enum for all available chat types that a player can select while playing
     */
    public enum ChatType {

        LOBBY_ONLY, PLAYERS_ONLY, PUBLIC

    }
}
