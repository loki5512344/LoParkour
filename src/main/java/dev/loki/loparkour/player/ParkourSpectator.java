package dev.loki.loparkour.player;

import dev.lolib.scheduler.Scheduler;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourSpectateEvent;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.player.data.PreviousData;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.util.ColorUtil;
import dev.lolib.scheduler.Scheduler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import dev.lolib.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Class for spectators of a Session.
 *
 * @author loki
 */
public class ParkourSpectator extends ParkourUser {

    private final dev.lolib.scheduler.ScheduledTask closestChecker;
    /**
     * The closest player.
     */
    @NotNull
    public ParkourPlayer closest;

    public ParkourSpectator(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, session, previousData);

        this.closest = session.getPlayers().get(0);

        new ParkourSpectateEvent(this).call();

        Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
                teleport(closest.getLocation());

                sendTranslated("play.spectator.join");

                player.setGameMode(GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
                if (ParkourUser.isBedrockPlayer(player)) {  // bedrock has no spectator mode, so just make the player invisible
                    player.setInvisible(true);
                    player.setCollidable(false);
                }
            }, 1);

        closestChecker = Scheduler.get(LoParkour.getPlugin()).runTimerAsync(() -> {
                if (session.getPlayers().isEmpty()) {
                    return;
                }

                closest = session.getPlayers().stream()
                        .min(Comparator.comparing(other -> other.getLocation().distanceSquared(player.getLocation()))) // x or x^2 doesn't matter in getting smallest
                        .orElse(closest);
            }, 1, 10);
    }

    /**
     * Updates the spectator's action bar, scoreboard and checks distance.
     */
    public void update() {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ColorUtil.color(Locales.getString(player, "play.spectator.action_bar"))));
        player.setGameMode(GameMode.SPECTATOR);
        updateScoreboard(session.generator);

        // spectator is still being teleported to world
        if (closest.getLocation().getWorld() != player.getLocation().getWorld()) {
            return;
        }

        if (closest.getLocation().distanceSquared(player.getLocation()) < 100 * 100) { // avoid sqrt
            return;
        }

        teleport(closest.getLocation());
        if (player.getGameMode() != GameMode.SPECTATOR) { // if player isn't in spectator or is a bedrock player
            return;
        }

        // if player is a spectator
        player.setSpectatorTarget(null);
        player.setSpectatorTarget(player.getSpectatorTarget());
    }

    /**
     * Stops the closest checker runnable.
     */
    @Override
    public void unregister() {
        closestChecker.cancel();
        session.removeSpectators(this);
        player.setInvisible(false);
    }
}
