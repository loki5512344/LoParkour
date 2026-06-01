package dev.loki.loparkour.mode.base;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.locale.Locales;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Shared mode messages from locale files.
 */
public final class ModeMessages {

    private ModeMessages() {
    }

    /**
     * @return true if joining is allowed; otherwise sends {@code other.joining_disabled} and returns false
     */
    public static boolean checkJoiningEnabled(@NotNull Player player) {
        if (Config.CONFIG.getBoolean("joining")) {
            return true;
        }
        Locales.send(player, "other.joining_disabled");
        return false;
    }
}
