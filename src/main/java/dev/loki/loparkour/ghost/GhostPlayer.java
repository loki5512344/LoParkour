package dev.loki.loparkour.ghost;

import dev.loki.loparkour.LoParkour;
import dev.efnilite.vilib.util.Task;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class GhostPlayer {

    private final GhostData data;
    private final World world;
    private ArmorStand entity;
    private BukkitTask task;
    private int currentFrame = 0;
    private long startTime;

    public GhostPlayer(@NotNull GhostData data, @NotNull World world) {
        this.data = data;
        this.world = world;
    }

    public void spawn(@NotNull Location startLocation) {
        if (data.getFrames().isEmpty()) {
            return;
        }

        entity = (ArmorStand) world.spawnEntity(startLocation, EntityType.ARMOR_STAND);
        entity.setVisible(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setCustomName("§7" + data.getPlayerName() + " §8(Ghost)");
        entity.setCustomNameVisible(true);

        startTime = System.currentTimeMillis();
        currentFrame = 0;

        task = Task.create(LoParkour.getPlugin())
            .repeat(1)
            .execute(this::update)
            .run();
    }

    private void update() {
        if (entity == null || !entity.isValid()) {
            stop();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        while (currentFrame < data.getFrames().size()) {
            GhostFrame frame = data.getFrames().get(currentFrame);

            if (frame.getTimestamp() > elapsed) {
                break;
            }

            Location loc = frame.toLocation(world);
            entity.teleport(loc);
            currentFrame++;
        }

        if (currentFrame >= data.getFrames().size()) {
            stop();
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (entity != null) {
            entity.remove();
            entity = null;
        }
    }

    public boolean isActive() {
        return entity != null && entity.isValid();
    }

    public String getPlayerName() {
        return data.getPlayerName();
    }

    public int getScore() {
        return data.getScore();
    }
}
