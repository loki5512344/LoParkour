package dev.loki.loparkour.generator;

import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.List;

/**
 * Handles particle and sound effects for the parkour generator.
 * Extracted from ParkourGenerator to keep it focused on generation logic.
 */
public class EffectManager {

    private final ParkourGenerator generator;

    public EffectManager(ParkourGenerator generator) {
        this.generator = generator;
    }

    /**
     * Spawns particles around the given blocks based on the configured shape.
     */
    public void particles(List<Block> blocks) {
        if (!generator.profile.get("particles").asBoolean()) return;

        List<Location> locations = blocks.stream().map(Block::getLocation).toList();
        Location max = locations.stream().reduce((a, b) -> {
            double x = Math.max(a.getX(), b.getX());
            double y = Math.max(a.getY(), b.getY());
            double z = Math.max(a.getZ(), b.getZ());
            return new Location(a.getWorld(), x, y, z);
        }).orElse(null);
        Location min = locations.stream().reduce((a, b) -> {
            double x = Math.min(a.getX(), b.getX());
            double y = Math.min(a.getY(), b.getY());
            double z = Math.min(a.getZ(), b.getZ());
            return new Location(a.getWorld(), x, y, z);
        }).orElse(null);

        if (max == null || min == null || max.getWorld() == null) return;

        List<ParkourPlayer> viewers = generator.getPlayers();

        switch (Option.PARTICLE_SHAPE) {
            case DOT -> {
                Location center = min.clone().add(
                    (max.getX() - min.getX()) / 2,
                    (max.getY() - min.getY()) / 2,
                    (max.getZ() - min.getZ()) / 2
                );
                viewers.forEach(p -> p.player.spawnParticle(
                    Option.PARTICLE_TYPE, center, 5, 0.1, 0.1, 0.1, 0,
                    Option.PARTICLE_DATA != null ? Option.PARTICLE_DATA.data() : null
                ));
            }
            case CIRCLE -> ParticleUtil.circle(
                min.clone().add(0.5, 0.5, 0.5), max.getWorld(), Option.PARTICLE_TYPE,
                viewers.stream().map(p -> p.player).toList(), 0.5, 8
            );
            case BOX -> ParticleUtil.box(
                org.bukkit.util.BoundingBox.of(min, max), max.getWorld(), Option.PARTICLE_TYPE,
                viewers.isEmpty() ? null : viewers.get(0).player, 0.3
            );
        }
    }

    /**
     * Plays the configured sound at the first block's location.
     */
    public void sound(List<Block> blocks) {
        if (!generator.profile.get("sound").asBoolean() || blocks.isEmpty()) return;

        Location loc = blocks.get(0).getLocation();
        generator.getPlayers().forEach(p ->
            p.player.playSound(loc, Option.SOUND_TYPE, Option.SOUND_VOLUME, Option.SOUND_PITCH));
        generator.getSpectators().forEach(s ->
            s.player.playSound(loc, Option.SOUND_TYPE, Option.SOUND_VOLUME, Option.SOUND_PITCH));
    }
}
