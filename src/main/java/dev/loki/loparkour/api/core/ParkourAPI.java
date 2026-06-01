package dev.loki.loparkour.api.core;

import dev.loki.loparkour.api.event.ParkourBlockGenerateEvent;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.player.core.ParkourUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Main API handler.
 * <ul>
 *     <li>For general player handling, please view {@link ParkourUser}.</li>
 *     <li>For parkour players, please view {@link ParkourPlayer}.</li>
 *     <li>For player spectating, please view {@link dev.efnilite.LoParkour.player.ParkourSpectator}.</li>
 *     <li>For events, please view {@link ParkourBlockGenerateEvent} and others in the events package.</li>
 *     <li>For Sessions, please view {@link dev.efnilite.LoParkour.session.Session}.</li>
 *     <li>For jump structures, see {@link dev.loki.loparkour.schematic.core.ParkourSchematic}</li>
 * </ul>
 */
public class ParkourAPI {

    private ParkourAPI() throws IllegalAccessException {
        throw new IllegalAccessException("Initializing API class");
    }

    /**
     * @param player The player.
     * @return The {@link ParkourPlayer}. Null if not found.
     */
    public static @Nullable ParkourPlayer getPlayer(Player player) {
        return ParkourPlayer.getPlayer(player);
    }

    /**
     * @param player The player.
     * @return The {@link ParkourPlayer}. Null if not found.
     */
    public static @Nullable ParkourUser getUser(Player player) {
        return ParkourUser.getUser(player);
    }
}
