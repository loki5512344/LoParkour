package dev.loki.loparkour.ghost;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GhostRecorder {

    private static final double MIN_MOVEMENT = 0.1;

    private final List<GhostFrame> frames = new ArrayList<>();
    private Location lastLocation;
    private long startTime;
    private boolean recording = false;

    public void startRecording(@NotNull Location startLocation) {
        frames.clear();
        lastLocation = startLocation.clone();
        startTime = System.currentTimeMillis();
        recording = true;
        
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

    public GhostData stopRecording(String playerName, int score) {
        recording = false;
        return new GhostData(playerName, score, frames);
    }

    public boolean isRecording() {
        return recording;
    }

    public int getFrameCount() {
        return frames.size();
    }
}
