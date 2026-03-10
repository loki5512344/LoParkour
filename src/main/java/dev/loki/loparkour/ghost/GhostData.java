package dev.loki.loparkour.ghost;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GhostData {

    private final String playerName;
    private final String playerUUID;
    private final int score;
    private final List<GhostFrame> frames;

    public GhostData(@NotNull String playerName, @NotNull String playerUUID, int score, @NotNull List<GhostFrame> frames) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.score = score;
        this.frames = new ArrayList<>(frames);
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public int getScore() {
        return score;
    }

    public List<GhostFrame> getFrames() {
        return frames;
    }

    public void saveToFile(@NotNull File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeUTF(playerName);
            out.writeUTF(playerUUID);
            out.writeInt(score);
            out.writeInt(frames.size());

            for (GhostFrame frame : frames) {
                out.writeLong(frame.getTimestamp());
                out.writeDouble(frame.getX());
                out.writeDouble(frame.getY());
                out.writeDouble(frame.getZ());
                out.writeFloat(frame.getYaw());
                out.writeFloat(frame.getPitch());
            }
        }
    }

    public static GhostData loadFromFile(@NotNull File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            String playerName = in.readUTF();
            String playerUUID = in.readUTF();
            int score = in.readInt();
            int frameCount = in.readInt();

            List<GhostFrame> frames = new ArrayList<>(frameCount);
            for (int i = 0; i < frameCount; i++) {
                long timestamp = in.readLong();
                double x = in.readDouble();
                double y = in.readDouble();
                double z = in.readDouble();
                float yaw = in.readFloat();
                float pitch = in.readFloat();

                frames.add(new GhostFrame(timestamp, x, y, z, yaw, pitch));
            }

            return new GhostData(playerName, playerUUID, score, frames);
        }
    }
}
