package dev.loki.loparkour.config;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.util.ParticleData;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Particle and sound settings. Populated by {@link Option#init}.
 */
public final class ParticleOptions {

    public static Option.ParticleShape SHAPE;
    public static Particle             PARTICLE_TYPE;
    public static ParticleData<?>      PARTICLE_DATA;
    public static Sound                SOUND_TYPE;
    public static int                  SOUND_PITCH;
    public static int                  SOUND_VOLUME;

    static void init() {
        String sound = Config.CONFIG.getString("particles.sound-type").toUpperCase();
        try {
            SOUND_TYPE = Sound.valueOf(sound);
        } catch (IllegalArgumentException ex) {
            SOUND_TYPE = Sound.valueOf("BLOCK_NOTE_PLING");
            LoParkour.getPlugin().getLogger().severe("Invalid sound: " + sound);
        }

        String particle = Config.CONFIG.getString("particles.particle-type");
        try {
            PARTICLE_TYPE = Particle.valueOf(particle);
        } catch (IllegalArgumentException ex) {
            PARTICLE_TYPE = Particle.valueOf("SPELL_INSTANT");
            LoParkour.getPlugin().getLogger().severe("Invalid particle: " + particle);
        }

        SOUND_PITCH    = Config.CONFIG.getInt("particles.sound-pitch");
        SOUND_VOLUME   = Config.CONFIG.getInt("particles.sound-volume");
        SHAPE          = Option.ParticleShape.valueOf(Config.CONFIG.getString("particles.particle-shape").toUpperCase());
        PARTICLE_DATA  = new ParticleData<>(PARTICLE_TYPE, null, 10);
    }

    private ParticleOptions() {}
}
