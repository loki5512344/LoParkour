package dev.loki.loparkour.ghost;

import dev.lolib.scheduler.Scheduler;

import dev.loki.loparkour.LoParkour;
import dev.lolib.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import dev.lolib.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

public class GhostPlayer {

    private final GhostData data;
    private final World world;
    private ArmorStand entity;
    private dev.lolib.scheduler.ScheduledTask task;
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

        task = Scheduler.get(LoParkour.getPlugin()).runTimer(this::update, 0, 1);
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
