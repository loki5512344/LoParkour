package dev.loki.loparkour.menu.core;

import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.options.Option;
import org.bukkit.permissions.Permissible;

/**
 * An enum for all Parkour Menu Options
 */
public enum ParkourOption {

    // main
    MAIN("main", "LoParkour.main"),

    // play
    PLAY("play", "LoParkour.play"),
    SINGLE("play.single", "LoParkour.play.single"),
    SPECTATOR("play.spectator", "LoParkour.play.spectator"),

    // community
    COMMUNITY("community", "LoParkour.community"),
    LEADERBOARDS("community.leaderboards", "LoParkour.community.leaderboards"),

    // settings
    SETTINGS("settings", "LoParkour.settings"),

    PARKOUR_SETTINGS("settings.parkour_settings.item", "LoParkour.settings.parkour_settings"),
    STYLES("settings.parkour_settings.items.styles", "LoParkour.settings.styles"),
    LEADS("settings.parkour_settings.items.leads", "LoParkour.settings.leads"),
    TIME("settings.parkour_settings.items.time", "LoParkour.settings.time"),
    SCHEMATICS("settings.parkour_settings.items.schematics", "LoParkour.settings.schematics"),
    SCOREBOARD("settings.parkour_settings.items.scoreboard", "LoParkour.settings.show_scoreboard"),
    FALL_MESSAGE("settings.parkour_settings.items.fall_message", "LoParkour.settings.fall_message"),
    PARTICLES("settings.parkour_settings.items.particles", "LoParkour.settings.particles"),
    SOUND("settings.parkour_settings.items.sound", "LoParkour.settings.sound"),
    SPECIAL_BLOCKS("settings.parkour_settings.items.special_blocks", "LoParkour.settings.special_blocks"),

    LANG("settings.lang", "LoParkour.settings.lang"),
    CHAT("settings.chat", "LoParkour.settings.chat"),

    // lobby
    LOBBY("lobby", "LoParkour.lobby"),
    VISIBILITY("lobby.visibility", "LoParkour.lobby.visibility"),
    PLAYER_MANAGEMENT("lobby.player_management", "LoParkour.lobby.player_management"),

    // other
    JOIN("join", "LoParkour.join"),
    QUIT("quit", "LoParkour.quit"),
    ADMIN("admin", "LoParkour.admin");

    /**
     * The path in config files for this option.
     */
    public final String path;

    /**
     * The permission for this option.
     */
    public final String permission;

    ParkourOption(String path, String permission) {
        this.path = path;
        this.permission = permission;
    }

    /**
     * @param permissible The player
     * @return True if the player is allowed to view/perform this option, false if not.
     */
    public boolean mayPerform(Permissible permissible) {
        boolean value = Option.OPTIONS_ENABLED.getOrDefault(this, true);

        if (value) {
            if (Config.CONFIG.getBoolean("permissions.enabled")) {
                return permissible.hasPermission(permission);
            }
            return true;
        }
        return false;
    }
}
