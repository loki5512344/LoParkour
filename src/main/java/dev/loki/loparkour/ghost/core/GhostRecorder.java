package dev.loki.loparkour.ghost.core;
import dev.loki.loparkour.ghost.model.GhostFrame;
import dev.loki.loparkour.ghost.model.GhostData;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Records a player's movement as a sequence of {@link GhostFrame}s.
 *
 * <p>Bugs fixed vs original:
 * <ul>
 *   <li>{@code stopRecording} took only {@code playerName} — now also takes {@code playerUuid}
 *       so {@link GhostData} can store it and {@link GhostManager} can name files by UUID.</li>
 * </ul>
 */
public class GhostRecorder {

    private static final double MIN_MOVEMENT = 0.1;

    private final List<GhostFrame> frames = new ArrayList<>();
    private Location lastLocation;
    private long startTime;
    private boolean recording = false;

    public void startRecording(@NotNull Location startLocation) {
        frames.clear();
        lastLocation = startLocation.clone();
        startTime    = System.currentTimeMillis();
        recording    = true;
        frames.add(new GhostFrame(0, startLocation.clone()));
    }

    public void recordFrame(@NotNull Location currentLocation) {
        if (!recording) {
            return;
        }
        if (lastLocation.distance(currentLocation) < MIN_MOVEMENT) {
            return;
        }

        long timestamp = System.currentTimeMillis() - startTime;
        frames.add(new GhostFrame(timestamp, currentLocation.clone()));
        lastLocation = currentLocation.clone();
    }

    /**
     * Stops recording and returns the completed ghost data.
     *
     * @param playerUuid UUID of the player — used as the filename in {@link GhostManager}.
     * @param playerName Display name shown on the ghost label in-game.
     * @param score      The score achieved this run.
     */
    public GhostData stopRecording(@NotNull UUID playerUuid,
                                   @NotNull String playerName,
                                   int score) {
        recording = false;
        return new GhostData(playerUuid, playerName, score, frames);
    }

    public boolean isRecording() {
        return recording;
    }

    public int getFrameCount() {
        return frames.size();
    }
}
