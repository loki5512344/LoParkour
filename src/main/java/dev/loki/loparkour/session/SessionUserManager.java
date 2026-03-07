package dev.loki.loparkour.session;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.player.ParkourUser;

import java.util.*;

/**
 * Manages users (players and spectators) within a session.
 *
 * @since 5.0.0
 */
class SessionUserManager {

    private final Map<UUID, ParkourUser> users = new HashMap<>();
    private final List<ParkourUser> muted = new ArrayList<>();
    private final Session session;

    public SessionUserManager(Session session) {
        this.session = session;
    }

    public void addPlayers(ParkourPlayer... toAdd) {
        for (ParkourPlayer player : toAdd) {
            notifyJoin(player);
            users.put(player.getUUID(), player);
        }
    }

    public void removePlayers(ParkourPlayer... toRemove) {
        for (ParkourPlayer player : toRemove) {
            users.remove(player.getUUID());
        }

        List<ParkourPlayer> remainingPlayers = getPlayers();
        notifyLeave(toRemove, remainingPlayers);

        if (toRemove.length > 0 && remainingPlayers.isEmpty()) {
            session.onAllPlayersLeft();
        }
    }

    public void addSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            notifySpectatorJoin(spectator);
            users.put(spectator.getUUID(), spectator);
        }
    }

    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            notifySpectatorLeave(spectator);
            users.remove(spectator.getUUID());
        }
    }

    public List<ParkourPlayer> getPlayers() {
        return users.values().stream()
                .filter(user -> user instanceof ParkourPlayer)
                .map(user -> (ParkourPlayer) user)
                .toList();
    }

    public List<ParkourSpectator> getSpectators() {
        return users.values().stream()
                .filter(user -> user instanceof ParkourSpectator)
                .map(user -> (ParkourSpectator) user)
                .toList();
    }

    public List<ParkourUser> getUsers() {
        return new ArrayList<>(users.values());
    }

    public void toggleMute(ParkourUser user) {
        if (!muted.remove(user)) {
            muted.add(user);
        }
    }

    public boolean isMuted(ParkourUser user) {
        return muted.contains(user);
    }

    private void notifyJoin(ParkourPlayer player) {
        for (ParkourPlayer to : getPlayers()) {
            to.send(Locales.getString(player.locale, "lobby.other_join").formatted(player.getName()));
        }
    }

    private void notifyLeave(ParkourPlayer[] toRemove, List<ParkourPlayer> remainingPlayers) {
        for (ParkourPlayer player : toRemove) {
            for (ParkourPlayer to : remainingPlayers) {
                to.send(Locales.getString(player.locale, "lobby.other_leave").formatted(player.getName()));
            }
        }
    }

    private void notifySpectatorJoin(ParkourSpectator spectator) {
        for (ParkourPlayer player : getPlayers()) {
            player.sendTranslated("play.spectator.other_join", spectator.getName());
        }
    }

    private void notifySpectatorLeave(ParkourSpectator spectator) {
        for (ParkourPlayer player : getPlayers()) {
            player.sendTranslated("play.spectator.other_leave", spectator.getName());
        }
    }
}
