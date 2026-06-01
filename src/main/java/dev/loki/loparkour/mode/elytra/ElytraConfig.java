package dev.loki.loparkour.mode.elytra;

import dev.loki.loparkour.config.core.Config;

/**
 * Configuration for Elytra mode loaded from config.yml
 */
public class ElytraConfig {

    private final int ringDistanceMin;
    private final int ringDistanceMax;
    private final int ringSize;
    private final int ringLead;
    private final int maxHeadingChangeHorizontal;
    private final int maxHeadingChangeVertical;
    private final int maxHeightAboveSpawn;
    private final int maxDeviation;
    private final double boostPower;
    private final long fireworkCooldownMs;
    private final int startingFireworks;

    public ElytraConfig() {
        this.ringDistanceMin = Config.CONFIG.isPath("modes.elytra.ring-distance-min")
            ? Config.CONFIG.getInt("modes.elytra.ring-distance-min") : 20;
        this.ringDistanceMax = Config.CONFIG.isPath("modes.elytra.ring-distance-max")
            ? Config.CONFIG.getInt("modes.elytra.ring-distance-max") : 40;
        this.ringSize = Config.CONFIG.isPath("modes.elytra.ring-size")
            ? Config.CONFIG.getInt("modes.elytra.ring-size") : 5;
        this.ringLead = Config.CONFIG.isPath("modes.elytra.ring-lead")
            ? Config.CONFIG.getInt("modes.elytra.ring-lead") : 8;
        this.maxHeadingChangeHorizontal = Config.CONFIG.isPath("modes.elytra.max-heading-change-horizontal")
            ? Config.CONFIG.getInt("modes.elytra.max-heading-change-horizontal") : 30;
        this.maxHeadingChangeVertical = Config.CONFIG.isPath("modes.elytra.max-heading-change-vertical")
            ? Config.CONFIG.getInt("modes.elytra.max-heading-change-vertical") : 15;
        this.maxHeightAboveSpawn = Config.CONFIG.isPath("modes.elytra.max-height-above-spawn")
            ? Config.CONFIG.getInt("modes.elytra.max-height-above-spawn") : 60;
        this.maxDeviation = Config.CONFIG.isPath("modes.elytra.max-deviation")
            ? Config.CONFIG.getInt("modes.elytra.max-deviation") : 25;
        this.boostPower = Config.CONFIG.isPath("modes.elytra.boost-power")
            ? Config.CONFIG.getDouble("modes.elytra.boost-power") : 1.5;
        this.fireworkCooldownMs = Config.CONFIG.isPath("modes.elytra.firework-cooldown-ms")
            ? Config.CONFIG.getInt("modes.elytra.firework-cooldown-ms") : 3000;
        this.startingFireworks = Config.CONFIG.isPath("modes.elytra.starting-fireworks")
            ? Config.CONFIG.getInt("modes.elytra.starting-fireworks") : 64;
    }

    public int getRingDistanceMin() { return ringDistanceMin; }
    public int getRingDistanceMax() { return ringDistanceMax; }
    public int getRingSize() { return ringSize; }
    public int getRingLead() { return ringLead; }
    public int getMaxHeadingChangeHorizontal() { return maxHeadingChangeHorizontal; }
    public int getMaxHeadingChangeVertical() { return maxHeadingChangeVertical; }
    public int getMaxHeightAboveSpawn() { return maxHeightAboveSpawn; }
    public int getMaxDeviation() { return maxDeviation; }
    public double getBoostPower() { return boostPower; }
    public long getFireworkCooldownMs() { return fireworkCooldownMs; }
    public int getStartingFireworks() { return startingFireworks; }
}
