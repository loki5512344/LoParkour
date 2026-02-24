package dev.loki.loparkour.schematic.lpschem;

import java.util.ArrayList;
import java.util.List;

public class SchematicVisuals {

    private List<BlockDisplay> displays;
    private List<ParticleEffect> particles;

    public SchematicVisuals() {
        this.displays = new ArrayList<>();
        this.particles = new ArrayList<>();
    }

    public List<BlockDisplay> getDisplays() {
        return displays;
    }

    public void addDisplay(BlockDisplay display) {
        displays.add(display);
    }

    public List<ParticleEffect> getParticles() {
        return particles;
    }

    public void addParticle(ParticleEffect particle) {
        particles.add(particle);
    }

    public static class BlockDisplay {
        public String type;
        public String block;
        public float[] pos;
        public float[] scale;
        public Animation animation;

        public BlockDisplay(String block, float[] pos, float[] scale) {
            this.type = "block";
            this.block = block;
            this.pos = pos;
            this.scale = scale;
        }
    }

    public static class Animation {
        public String type;
        public String axis;
        public float speed;

        public Animation(String type, String axis, float speed) {
            this.type = type;
            this.axis = axis;
            this.speed = speed;
        }
    }

    public static class ParticleEffect {
        public String type;
        public float[] pos;
        public int amount;
        public float spread;

        public ParticleEffect(String type, float[] pos, int amount, float spread) {
            this.type = type;
            this.pos = pos;
            this.amount = amount;
            this.spread = spread;
        }
    }
}
