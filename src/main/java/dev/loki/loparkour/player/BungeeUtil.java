package dev.loki.loparkour.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.loki.loparkour.LoParkour;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;

/**
 * Utility for BungeeCord operations.
 *
 * @since 5.0.0
 */
class BungeeUtil {

    public static void sendPlayerToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        try {
            player.sendPluginMessage(LoParkour.getPlugin(), "BungeeCord", out.toByteArray());
        } catch (ChannelNotRegisteredException ex) {
            LoParkour.getPlugin().getLogger().severe(
                    "Error while trying to send %s to server %s. - %s".formatted(player.getName(), server, ex.getMessage()));
            player.kickPlayer("Couldn't move you to %s. Please rejoin.".formatted(server));
        }
    }
}
