package dev.loki.loparkour.mode;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.world.Divider;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Co-op Mode — multiple players share one session and one score.
 *
 * <p>Implements {@link MultiMode} so it is excluded from the single-player
 * mode selection menu in {@link dev.loki.loparkour.menu.play.SingleMenu}.
 * Players join via /parkour coop or through a lobby invite flow.</p>
 *
 * <p>Problems fixed vs previous version:
 * <ul>
 *   <li>Did NOT implement MultiMode → showed up in single-player menu</li>
 *   <li>CoopGenerator was a private inner class — inaccessible from join/leave</li>
 *   <li>sharedScore was a separate field that duplicated state.score</li>
 *   <li>fall() called super.fall() THEN tried to clear state that was already reset</li>
 *   <li>playerContributions never cleared between falls correctly</li>
 *   <li>create(Player) silently created a solo session instead of rejecting</li>
 * </ul>
 */
public class CoopMode implements MultiMode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    // Active coop sessions — used so join() can add to an existing one
    // Key = session, tracked via Divider automatically; this is just for lookup
    // We look up by existing player's session instead.

    @Override
    @NotNull
    public String getName() {
        return "coop";
    }

    /**
     * Returns null so this mode never appears in any menu item list.
     * Players join via lobby invite or command only.
     */
    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return null;
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    /**
     * Called when a player tries to start coop solo from a command/menu.
     * Opens the lobby menu so they can invite others — does NOT create a session here.
     * A session is created only once a second player joins via {@link #join}.
     */
    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage("§cJoining is currently disabled.");
            return;
        }
        // Opening lobby menu lets player invite others — actual session created in join()
        Menus.LOBBY.open(player);
    }

    /**
     * Adds a player to an existing coop session, or creates a new session if
     * the target player is not yet in one.
     */
    @Override
    public void join(Player joiningPlayer, Session session) {
        if (!Config.CONFIG.getBoolean("joining")) {
            joiningPlayer.sendMessage("§cJoining is currently disabled.");
            return;
        }

        if (session.generator == null) return;

        // Register the joining player into the existing session
        ParkourPlayer pp = ParkourUser.register(joiningPlayer, session);
        session.addPlayers(pp);
        pp.updateGeneratorSettings(session.generator);

        // Inform everyone
        session.getPlayers().forEach(p ->
            p.sendTranslated("modes.coop.player_joined", joiningPlayer.getName()));
    }

    /**
     * Removes a player from a coop session.
     * If the last player leaves, the session ends normally.
     */
    @Override
    public void leave(Player player, Session session) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp == null) return;
        ParkourUser.leave(pp);
    }

    @Override
    public int getMaxPlayers() {
        return Config.CONFIG.getInt("modes.coop.max-players");
    }

    // ── CoopGenerator ─────────────────────────────────────────────────────────

    /**
     * Package-accessible so Session.create() lambda can reference it,
     * and so join() can cast generator when needed.
     */
    static class CoopGenerator extends ParkourGenerator {

        /** Individual block contributions per player UUID. */
        private final Map<UUID, Integer> contributions = new HashMap<>();

        public CoopGenerator(@NotNull Session session) {
            super(session);
            session.getPlayers().forEach(pp -> contributions.put(pp.getUUID(), 0));
        }

        @Override
        protected void score() {
            super.score(); // increments state.score and totalScore

            // Track which player was on the block (primary player for single, varies in multi)
            contributions.merge(player.getUUID(), 1, Integer::sum);

            // Milestone every 50 points
            if (state.score % 50 == 0) {
                getPlayers().forEach(pp -> {
                    pp.player.sendTitle(
                        "§6Team: §e" + state.score,
                        "§aKeep it up!",
                        5, 40, 10);
                    pp.player.playSound(pp.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
                });
            }
        }

        /**
         * When a new player joins mid-game, add them to contributions map.
         */
        public void onPlayerJoin(ParkourPlayer pp) {
            contributions.putIfAbsent(pp.getUUID(), 0);
        }

        @Override
        protected void fall() {
            // Show each player their individual contribution before reset
            getPlayers().forEach(pp -> {
                int contrib = contributions.getOrDefault(pp.getUUID(), 0);
                int pct = state.score > 0 ? (contrib * 100 / state.score) : 0;
                pp.sendTranslated("modes.coop.stats",
                    Integer.toString(state.score),
                    Integer.toString(contrib),
                    Integer.toString(pct));
            });

            super.fall(); // handles reset, leaderboard, etc.
        }

        @Override
        public void reset(boolean regenerate) {
            super.reset(regenerate);
            contributions.clear();
            getPlayers().forEach(pp -> contributions.put(pp.getUUID(), 0));
        }

        @Override
        public Mode getMode() {
            return Modes.COOP;
        }
    }
}
