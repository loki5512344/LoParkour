package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.config.Config;

/**
 * Configuration settings for Elytra mode.
 */
public class ElytraConfig {
    
    private final double ringDistanceMin;
    private final double ringDistanceMax;
    private final double ringSize;
    private final int ringLead;
    private final double maxHeadingChangeHorizontal;
    private final double maxHeadingChangeVertical;
    private final double maxHeightAboveSpawn;
    private final double maxDeviation;
    private final double boostPower;
    private final long fireworkCooldownMs;
    private final int startingFireworks;
    
    public ElytraConfig() {
        this.ringDistanceMin = Config.CONFIG.getDouble("modes.elytra.ring-distance-min");
        this.ringDistanceMax = Config.CONFIG.getDouble("modes.elytra.ring-distance-max");
        this.ringSize = Config.CONFIG.getDouble("modes.elytra.ring-size");
        this.ringLead = Config.CONFIG.getInt("modes.elytra.ring-lead");
        this.maxHeadingChangeHorizontal = Config.CONFIG.getDouble("modes.elytra.max-heading-change-horizontal");
        this.maxHeadingChangeVertical = Config.CONFIG.getDouble("modes.elytra.max-heading-change-vertical");
        this.maxHeightAboveSpawn = Config.CONFIG.getDouble("modes.elytra.max-height-above-spawn");
        this.maxDeviation = Config.CONFIG.getDouble("modes.elytra.max-deviation");
        this.boostPower = Config.CONFIG.getDouble("modes.elytra.boost-power");
        this.fireworkCooldownMs = (long) Config.CONFIG.getDouble("modes.elytra.firework-cooldown-ms");
        this.startingFireworks = Config.CONFIG.getInt("modes.elytra.starting-fireworks");
    }
    
    // Getters
    public double getRingDistanceMin() { return ringDistanceMin; }
    public double getRingDistanceMax() { return ringDistanceMax; }
    public double getRingSize() { return ringSize; }
    public int getRingLead() { return ringLead; }
    public double getMaxHeadingChangeHorizontal() { return maxHeadingChangeHorizontal; }
    public double getMaxHeadingChangeVertical() { return maxHeadingChangeVertical; }
    public double getMaxHeightAboveSpawn() { return maxHeightAboveSpawn; }
    public double getMaxDeviation() { return maxDeviation; }
    public double getBoostPower() { return boostPower; }
    public long getFireworkCooldownMs() { return fireworkCooldownMs; }
    public int getStartingFireworks() { return startingFireworks; }
}