package dev.loki.loparkour.ghost;

import java.util.ArrayList;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GhostManager {

    private static final File GHOST_FOLDER = LoParkour.getInFolder("ghosts");
    private static final int MAX_GHOSTS_PER_MODE = 3;

    private final Map<String, List<GhostData>> ghostsByMode = new HashMap<>();
    private final List<GhostPlayer> activeGhosts = new ArrayList<>();

    public GhostManager() {
        if (!GHOST_FOLDER.exists()) {
            GHOST_FOLDER.mkdirs();
        }
    }

    public void loadGhosts(@NotNull String mode) {
        File modeFolder = new File(GHOST_FOLDER, mode);
        if (!modeFolder.exists()) {
            return;
        }

        List<GhostData> ghosts = new ArrayList<>();
        File[] files = modeFolder.listFiles((dir, name) -> name.endsWith(".ghost"));

        if (files != null) {
            for (File file : files) {
                try {
                    GhostData data = GhostData.loadFromFile(file);
                    ghosts.add(data);
                } catch (IOException e) {
                    LoParkour.getPlugin().getLogger().severe("Failed to load ghost: " + file.getName());
                }
            }
        }

        ghosts.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        ghostsByMode.put(mode, ghosts);
    }

    public void saveGhost(@NotNull String mode, @NotNull GhostData data) {
        File modeFolder = new File(GHOST_FOLDER, mode);
        if (!modeFolder.exists()) {
            modeFolder.mkdirs();
        }

        List<GhostData> ghosts = ghostsByMode.getOrDefault(mode, new ArrayList<>());
        ghosts.add(data);
        ghosts.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        // Remove old ghosts beyond MAX limit
        while (ghosts.size() > MAX_GHOSTS_PER_MODE) {
            GhostData removed = ghosts.remove(ghosts.size() - 1);
            // Use UUID for filename to avoid collisions and path traversal
            File file = new File(modeFolder, removed.getPlayerUUID() + ".ghost");
            file.delete();
        }

        ghostsByMode.put(mode, ghosts);

        // Use UUID for filename instead of player name
        File file = new File(modeFolder, data.getPlayerUUID() + ".ghost");
        try {
            data.saveToFile(file);
        } catch (IOException e) {
            LoParkour.getPlugin().getLogger().severe("Failed to save ghost: " + file.getName());
        }
    }

    public void spawnGhosts(@NotNull String mode, @NotNull Location startLocation, @NotNull World world) {
        if (!Config.CONFIG.getBoolean("ghost-mode.enabled")) {
            return;
        }

        stopAllGhosts();

        List<GhostData> ghosts = ghostsByMode.get(mode);
        if (ghosts == null || ghosts.isEmpty()) {
            return;
        }

        int showTop = Math.min(Config.CONFIG.getInt("ghost-mode.show-top"), ghosts.size());
        for (int i = 0; i < showTop; i++) {
            GhostPlayer ghost = new GhostPlayer(ghosts.get(i), world);
            ghost.spawn(startLocation);
            activeGhosts.add(ghost);
        }
    }

    public void stopAllGhosts() {
        for (GhostPlayer ghost : activeGhosts) {
            ghost.stop();
        }
        activeGhosts.clear();
    }

    @Nullable
    public GhostData getTopGhost(@NotNull String mode) {
        List<GhostData> ghosts = ghostsByMode.get(mode);
        if (ghosts == null || ghosts.isEmpty()) {
            return null;
        }
        return ghosts.get(0);
    }

    public List<GhostData> getTopGhosts(@NotNull String mode, int count) {
        List<GhostData> ghosts = ghostsByMode.get(mode);
        if (ghosts == null || ghosts.isEmpty()) {
            return Collections.emptyList();
        }
        return ghosts.subList(0, Math.min(count, ghosts.size()));
    }

    public boolean shouldRecordGhost(@NotNull String mode, int score) {
        List<GhostData> ghosts = ghostsByMode.get(mode);
        if (ghosts == null || ghosts.size() < MAX_GHOSTS_PER_MODE) {
            return true;
        }

        GhostData lowestGhost = ghosts.get(ghosts.size() - 1);
        return score > lowestGhost.getScore();
    }
}
