package dev.loki.loparkour.menu.core;

import dev.loki.loparkour.menu.community.CommunityMenu;
import dev.loki.loparkour.menu.community.LeaderboardsMenu;
import dev.loki.loparkour.menu.community.SingleLeaderboardMenu;
import dev.loki.loparkour.menu.lobby.LobbyMenu;
import dev.loki.loparkour.menu.lobby.PlayerManagementMenu;
import dev.loki.loparkour.menu.play.PlayMenu;
import dev.loki.loparkour.menu.play.SingleMenu;
import dev.loki.loparkour.menu.play.SpectatorMenu;
import dev.loki.loparkour.menu.settings.LangMenu;
import dev.loki.loparkour.menu.settings.ParkourSettingsMenu;
import dev.loki.loparkour.menu.settings.SettingsMenu;

public final class Menus {

    private Menus() {
    }

    // main
    public static MainMenu MAIN = new MainMenu();

    // play
    public static PlayMenu PLAY = new PlayMenu();
    public static SingleMenu SINGLE = new SingleMenu();
    public static SpectatorMenu SPECTATOR = new SpectatorMenu();

    // community
    public static CommunityMenu COMMUNITY = new CommunityMenu();
    public static LeaderboardsMenu LEADERBOARDS = new LeaderboardsMenu();
    public static SingleLeaderboardMenu SINGLE_LEADERBOARD = new SingleLeaderboardMenu();

    // settings
    public static SettingsMenu SETTINGS = new SettingsMenu();
    public static LangMenu LANG = new LangMenu();
    public static ParkourSettingsMenu PARKOUR_SETTINGS = new ParkourSettingsMenu();

    // lobby
    public static LobbyMenu LOBBY = new LobbyMenu();
    public static PlayerManagementMenu PLAYER_MANAGEMENT = new PlayerManagementMenu();

}
