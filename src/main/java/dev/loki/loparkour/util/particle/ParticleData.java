package dev.loki.loparkour.util.particle;

import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Particle data holder for migration from vilib
 */
public class ParticleData<T> {
    private final Particle particle;
    private final T data;
    private int size = 1;
    private double speed = 0.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double offsetZ = 0.0;
    
    public ParticleData(@NotNull Particle particle, @Nullable T data, int size) {
        this.particle = particle;
        this.data = data;
        this.size = size;
    }
    
    public ParticleData<T> size(int size) {
        this.size = size;
        return this;
    }
    
    public ParticleData<T> speed(double speed) {
        this.speed = speed;
        return this;
    }
    
    public ParticleData<T> offsetX(double offsetX) {
        this.offsetX = offsetX;
        return this;
    }
    
    public ParticleData<T> offsetY(double offsetY) {
        this.offsetY = offsetY;
        return this;
    }
    
    public ParticleData<T> offsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
        return this;
    }
    
    public Particle getParticle() {
        return particle;
    }
    
    public T getData() {
        return data;
    }
    
    public int getSize() {
        return size;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public double getOffsetX() {
        return offsetX;
    }
    
    public double getOffsetY() {
        return offsetY;
    }
    
    public double getOffsetZ() {
        return offsetZ;
    }
}
