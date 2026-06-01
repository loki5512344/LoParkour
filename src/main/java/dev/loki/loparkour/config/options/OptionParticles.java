package dev.loki.loparkour.config.options;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.config.options.Option;
import dev.loki.loparkour.util.particle.ParticleData;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Particle and sound configuration options.
 */
public class OptionParticles {

    public static Option.ParticleShape PARTICLE_SHAPE;
    public static Sound                SOUND_TYPE;
    public static int                  SOUND_PITCH;
    public static int                  SOUND_VOLUME;
    public static Particle             PARTICLE_TYPE;
    public static ParticleData<?>      PARTICLE_DATA;

    public static void init() {
        SOUND_TYPE    = resolveSound(Config.CONFIG.getString("particles.sound-type"));
        PARTICLE_TYPE = resolveParticle(Config.CONFIG.getString("particles.particle-type"));
        PARTICLE_SHAPE = resolveShape(Config.CONFIG.getString("particles.particle-shape"));

        SOUND_PITCH   = Config.CONFIG.getInt("particles.sound-pitch");
        SOUND_VOLUME  = Config.CONFIG.getInt("particles.sound-volume");
        PARTICLE_DATA = new ParticleData<>(PARTICLE_TYPE, null, 10);
    }

    private static Sound resolveSound(String name) {
        if (name != null && !name.isEmpty()) {
            try { return Sound.valueOf(name.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        for (String fb : new String[]{"BLOCK_NOTE_BLOCK_GUITAR", "BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_PLING"}) {
            try { return Sound.valueOf(fb); }
            catch (IllegalArgumentException ignored) {}
        }
        LoParkour.getPlugin().getLogger().warning("Could not resolve any sound, using first available.");
        return Sound.values()[0];
    }

    private static Particle resolveParticle(String name) {
        if (name != null && !name.isEmpty()) {
            try { return Particle.valueOf(name.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        for (String fb : new String[]{"INSTANT_EFFECT", "SPELL_INSTANT", "CRIT"}) {
            try { return Particle.valueOf(fb); }
            catch (IllegalArgumentException ignored) {}
        }
        LoParkour.getPlugin().getLogger().warning("Could not resolve any particle, using first available.");
        return Particle.values()[0];
    }

    private static Option.ParticleShape resolveShape(String name) {
        if (name != null && !name.isEmpty()) {
            try { return Option.ParticleShape.valueOf(name.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return Option.ParticleShape.BOX;
    }
}
