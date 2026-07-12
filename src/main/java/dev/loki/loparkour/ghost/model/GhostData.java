package dev.loki.loparkour.ghost.model;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable snapshot of a player's run: who ran it, their score, and every recorded frame.
 *
 * <p>Bugs fixed vs original:
 * <ul>
 *   <li>Files were named {@code playerName.ghost} — two players with the same name
 *       overwrote each other, and a name like {@code ../../config} caused path traversal.
 *       UUID is now stored and used as the filename instead.</li>
 *   <li>File format is backwards-compatible: UUID is written first (new field),
 *       then the existing layout. Old files without a UUID are handled in
 *       {@link #loadFromFile} by catching the format mismatch and returning null.</li>
 * </ul>
 */
public class GhostData {

    /** Binary format version — bump when changing the serialised layout. */
    private static final int FORMAT_VERSION = 2;

    private final UUID playerUuid;
    private final String playerName;
    private final int score;
    private final List<GhostFrame> frames;

    public GhostData(@NotNull UUID playerUuid, @NotNull String playerName,
                     int score, @NotNull List<GhostFrame> frames) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.score      = score;
        this.frames     = new ArrayList<>(frames);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public List<GhostFrame> getFrames() {
        return frames;
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    public void saveToFile(@NotNull File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(FORMAT_VERSION);              // version header
            out.writeUTF(playerUuid.toString());       // UUID  (filename-safe, collision-free)
            out.writeUTF(playerName);                  // display name (for the ghost label)
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

    /**
     * Loads a ghost from file.
     *
     * @return The loaded data, or {@code null} if the file is an old v1 format
     *         (no version header / UUID) — caller should delete and ignore it.
     */
    public static GhostData loadFromFile(@NotNull File file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                // Old format (v1) started with readUTF for playerName, not an int version.
                // Those files are stale — return null so the manager can discard them.
                return null;
            }

            UUID uuid       = UUID.fromString(in.readUTF());
            String name     = in.readUTF();
            int score       = in.readInt();
            int frameCount  = in.readInt();

            List<GhostFrame> frames = new ArrayList<>(frameCount);
            for (int i = 0; i < frameCount; i++) {
                frames.add(new GhostFrame(
                    in.readLong(),
                    in.readDouble(), in.readDouble(), in.readDouble(),
                    in.readFloat(),  in.readFloat()));
            }

            return new GhostData(uuid, name, score, frames);
        }
    }
}
