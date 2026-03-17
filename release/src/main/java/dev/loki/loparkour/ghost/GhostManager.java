package dev.loki.loparkour.ghost;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Loads, saves, and spawns ghost runs per mode.
 *
 * <p>Bugs fixed vs original:
 * <ul>
 *   <li>Files were named {@code playerName.ghost} — collisions between players sharing
 *       a name, and path traversal via names like {@code ../../config}.
 *       Now named {@code uuid.ghost} — guaranteed unique and filesystem-safe.</li>
 *   <li>When evicting the lowest ghost, the old file was looked up by player name
 *       which failed if the name had changed since the run was saved.</li>
 *   <li>Old v1 files (no UUID) are detected via {@link GhostData#loadFromFile}
 *       returning null — they are deleted automatically on first load.</li>
 * </ul>
 */
public class GhostManager {

    private static final int MAX_GHOSTS_PER_MODE = 3;

    private final File ghostFolder;
    private final Map<String, List<GhostData>> ghostsByMode = new HashMap<>();
    private final List<GhostPlayer> activeGhosts = new ArrayList<>();

    public GhostManager() {
        this.ghostFolder = LoParkour.getInFolder("ghosts");
        if (!ghostFolder.exists()) {
            ghostFolder.mkdirs();
        }
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    public void loadGhosts(@NotNull String mode) {
        File modeFolder = new File(ghostFolder, sanitizeMode(mode));
        if (!modeFolder.exists()) return;

        File[] files = modeFolder.listFiles((dir, name) -> name.endsWith(".ghost"));
        if (files == null) return;

        List<GhostData> ghosts = new ArrayList<>();
        for (File file : files) {
            try {
                GhostData data = GhostData.loadFromFile(file);
                if (data == null) {
                    // Old v1 format — delete and skip
                    LoParkour.getPlugin().getLogger().info(
                        "Deleting outdated ghost file (old format): " + file.getName());
                    file.delete();
                    continue;
                }
                ghosts.add(data);
            } catch (IOException e) {
                LoParkour.getPlugin().getLogger().severe(
                    "Failed to load ghost file " + file.getName() + ": " + e.getMessage());
            }
        }

        ghosts.sort(Comparator.comparingInt(GhostData::getScore).reversed());
        ghostsByMode.put(mode, ghosts);
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    public void saveGhost(@NotNull String mode, @NotNull GhostData data) {
        File modeFolder = new File(ghostFolder, sanitizeMode(mode));
        if (!modeFolder.exists()) modeFolder.mkdirs();

        List<GhostData> ghosts = new ArrayList<>(ghostsByMode.getOrDefault(mode, new ArrayList<>()));
        ghosts.add(data);
        ghosts.sort(Comparator.comparingInt(GhostData::getScore).reversed());

        // Evict lowest-score ghosts beyond the cap
        while (ghosts.size() > MAX_GHOSTS_PER_MODE) {
            GhostData evicted = ghosts.remove(ghosts.size() - 1);
            // File is named by UUID — always finds the right file regardless of name changes
            File evictedFile = ghostFile(modeFolder, evicted);
            if (evictedFile.exists()) evictedFile.delete();
        }

        ghostsByMode.put(mode, ghosts);

        // Save new ghost — named by UUID, not by player name
        File file = ghostFile(modeFolder, data);
        try {
            data.saveToFile(file);
        } catch (IOException e) {
            LoParkour.getPlugin().getLogger().severe(
                "Failed to save ghost for " + data.getPlayerName() + ": " + e.getMessage());
        }
    }

    // ── Spawning ──────────────────────────────────────────────────────────────

    public void spawnGhosts(@NotNull String mode, @NotNull Location startLocation,
                            @NotNull World world) {
        if (!Config.CONFIG.getBoolean("ghost-mode.enabled")) return;

        stopAllGhosts();

        List<GhostData> ghosts = ghostsByMode.get(mode);
        if (ghosts == null || ghosts.isEmpty()) return;

        int showTop = Math.min(Config.CONFIG.getInt("ghost-mode.show-top"), ghosts.size());
        for (int i = 0; i < showTop; i++) {
            GhostPlayer ghost = new GhostPlayer(ghosts.get(i), world);
            ghost.spawn(startLocation);
            activeGhosts.add(ghost);
        }
    }

    public void stopAllGhosts() {
        activeGhosts.forEach(GhostPlayer::stop);
        activeGhosts.clear();
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Nullable
    public GhostData getTopGhost(@NotNull String mode) {
        List<GhostData> ghosts = ghostsByMode.get(mode);
        return (ghosts == null || ghosts.isEmpty()) ? null : ghosts.get(0);
    }

    public List<GhostData> getTopGhosts(@NotNull String mode, int count) {
        List<GhostData> ghosts = ghostsByMode.get(mode);
        if (ghosts == null || ghosts.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(ghosts.subList(0, Math.min(count, ghosts.size())));
    }

    public boolean shouldRecordGhost(@NotNull String mode, int score) {
        List<GhostData> ghosts = ghostsByMode.get(mode);
        if (ghosts == null || ghosts.size() < MAX_GHOSTS_PER_MODE) return true;
        return score > ghosts.get(ghosts.size() - 1).getScore();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Returns the ghost file for a run — always UUID-based, never name-based. */
    private File ghostFile(File modeFolder, GhostData data) {
        return new File(modeFolder, data.getPlayerUuid().toString() + ".ghost");
    }

    /**
     * Strips any path separators from mode names so they can't be used to
     * escape the ghosts directory (e.g. mode name "../../etc").
     */
    private static String sanitizeMode(String mode) {
        return mode.replaceAll("[/\\\\.]", "_");
    }
}
