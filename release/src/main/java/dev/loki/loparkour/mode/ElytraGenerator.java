package dev.loki.loparkour.mode;

import dev.lolib.scheduler.Scheduler;
import dev.lolib.scheduler.ScheduledTask;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.event.ParkourFallEvent;
import dev.loki.loparkour.api.event.ParkourScoreEvent;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.world.Divider;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generator for Elytra mode.
 *
 * <p>Completely replaces block-based parkour with an aerial ring course:
 * <ul>
 *   <li>Rings are placed ahead along a smooth curved path in the sky</li>
 *   <li>Each ring is vertical, perpendicular to the flight direction</li>
 *   <li>Player must fly through the center — detection uses plane-crossing math</li>
 *   <li>Firework usage is intercepted via event to enforce cooldown properly</li>
 *   <li>Fall = player touches ground / loses flight / flies too far from course</li>
 * </ul>
 */
public class ElytraGenerator extends ParkourGenerator implements Listener {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** How many rings behind the player to keep before deleting. */
    private static final int RING_TRAIL = 3;

    /** Ticks between particle ring renders (every 2 ticks = 10fps). */
    private static final int RENDER_INTERVAL = 2;

    /** Minimum Y height — if player drops below this they failed. */
    private static final double MIN_Y_OFFSET = -30.0;

    // Config-driven values cached at init
    private int ringLead;
    private double maxDeviation;

    /** Particle count per ring point. */
    private static final int PARTICLES_PER_POINT = 1;

    /** Number of particle points around each ring. */
    private static final int RING_POINTS = 36;

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<ElytraRing> rings = new ArrayList<>();
    private int nextRingIndex = 0;         // index of the next ring the player hasn't passed
    private int tickCounter = 0;
    private Instant startTime = null;
    private int score = 0;

    private ScheduledTask tickTask;
    private ScheduledTask renderTask;

    /** Firework cooldown per player UUID in ms. */
    private final Map<UUID, Long> fireworkCooldowns = new HashMap<>();

    /** Starting Y for fall detection. */
    private double spawnY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ElytraGenerator(@NotNull Session session) {
        super(session);
        this.ringLead = Config.CONFIG.getInt("modes.elytra.ring-lead");
        this.maxDeviation = Config.CONFIG.getDouble("modes.elytra.max-deviation");
    }

    // ── Mode identification ───────────────────────────────────────────────────

    @Override
    public Mode getMode() {
        return Modes.ELYTRA;
    }

    // ── Lifecycle: called by Session after island.build() ────────────────────

    /**
     * Called once when the generator is set up.
     * We skip the block-based generateFirst and do our own aerial setup.
     */
    @Override
    public void generateFirst(Location spawn, Location blockSpawn) {
        spawnY = spawn.getY();
        state.playerSpawn = spawn;
        state.lastStandingPlayerLocation = spawn;
        state.blockSpawn = blockSpawn;

        // Seed the ring path starting just in front of spawn
        Vector initialHeading = state.heading.clone().normalize();
        Location ringOrigin = spawn.clone().add(initialHeading.clone().multiply(10)).add(0, 2, 0);

        generateRings(ringOrigin, initialHeading, ringLead);

        // Setup player
        for (ParkourPlayer pp : getPlayers()) {
            Player p = pp.player;
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();

            // Elytra chestplate
            p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));

            // Starting fireworks
            int amount = Config.CONFIG.getInt("modes.elytra.starting-fireworks");
            p.getInventory().setItem(0, makeFirework(amount));
            p.getInventory().setHeldItemSlot(0);

            // Launch into glide
            p.setVelocity(initialHeading.clone().multiply(1.2).setY(0.4));
            p.setGliding(true);

            pp.sendTranslated("modes.elytra.start_hint");
        }

        // Register firework event listener
        Bukkit.getPluginManager().registerEvents(this, LoParkour.getPlugin());

        // Start ticks
        tickTask = Scheduler.get(LoParkour.getPlugin()).runTimer(this::tick, 1, 1);
        renderTask = Scheduler.get(LoParkour.getPlugin()).runTimer(this::renderRings, 0, RENDER_INTERVAL);
    }

    // ── generateFirst is the entry — skip normal generate() completely ────────

    @Override
    public void generate() { /* not used in elytra mode */ }

    @Override
    public void generate(int amount) { /* not used */ }

    @Override
    public void startTick() { /* tick started in generateFirst */ }

    // ── Main tick ─────────────────────────────────────────────────────────────

    private void tick() {
        if (state.stopped) {
            stopTasks();
            return;
        }

        tickCounter++;

        for (ParkourPlayer pp : getPlayers()) {
            Player p = pp.player;

            // Ensure player stays gliding
            if (!p.isGliding() && p.getGameMode() == GameMode.ADVENTURE) {
                // Only force-glide if they're not on ground and have elytra
                if (!p.isOnGround()) {
                    p.setGliding(true);
                }
            }

            // Update scoreboard
            pp.updateScoreboard(this);
            p.setSaturation(20);

            // Action bar: score + next ring distance
            updateActionBar(pp);
        }

        // Fall detection
        checkFall();

        // Ring crossing detection
        checkRingCrossings();
    }

    // ── Ring generation ───────────────────────────────────────────────────────

    /**
     * Generates {@code count} new rings starting from {@code origin},
     * heading in {@code direction}, with smooth curve randomisation.
     */
    private void generateRings(Location origin, Vector direction, int count) {
        int distMin = Config.CONFIG.getInt("modes.elytra.ring-distance-min");
        int distMax = Config.CONFIG.getInt("modes.elytra.ring-distance-max");
        double ringSize = Config.CONFIG.getDouble("modes.elytra.ring-size");
        double maxYaw   = Config.CONFIG.getDouble("modes.elytra.max-heading-change-horizontal");
        double maxPitch = Config.CONFIG.getDouble("modes.elytra.max-heading-change-vertical");
        double maxHeight = Config.CONFIG.getDouble("modes.elytra.max-height-above-spawn");

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Location cursor = origin.clone();
        Vector heading = direction.clone().normalize();

        // If we already have rings, continue from the last one
        if (!rings.isEmpty()) {
            ElytraRing last = rings.get(rings.size() - 1);
            cursor = last.center.clone();
            heading = last.normal.clone();
        }

        for (int i = 0; i < count; i++) {
            // Distance between rings
            double dist = distMin + rng.nextDouble() * (distMax - distMin);

            // Smooth heading change using config-defined limits
            double yawDelta   = Math.toRadians(rng.nextDouble(-maxYaw, maxYaw));
            double pitchDelta = Math.toRadians(rng.nextDouble(-maxPitch, maxPitch));

            heading = rotateVector(heading, yawDelta, pitchDelta);
            heading.normalize();

            // New ring center
            Location center = cursor.clone().add(heading.clone().multiply(dist));

            // Keep rings in a reasonable Y band
            double targetY = Math.max(spawnY + 5, Math.min(spawnY + maxHeight, center.getY()));
            center.setY(targetY);

            rings.add(new ElytraRing(rings.size(), center.clone(), heading.clone(), ringSize));
            cursor = center;
        }
    }

    // ── Ring crossing detection ───────────────────────────────────────────────

    private void checkRingCrossings() {
        if (rings.isEmpty() || nextRingIndex >= rings.size()) return;

        ParkourPlayer pp = player; // primary player
        Location loc = pp.getLocation();

        ElytraRing nextRing = rings.get(nextRingIndex);

        // Check if player crossed the ring's plane
        if (nextRing.hasCrossed(loc, pp.player.getVelocity())) {
            if (nextRing.isInside(loc)) {
                // Perfect pass — score
                onRingPassed(pp, nextRing, true);
            } else {
                // Crossed the plane but missed the ring hole
                onRingMissed(pp, nextRing);
            }
            nextRingIndex++;

            // Generate more rings ahead
            int ahead = rings.size() - nextRingIndex;
            if (ahead < ringLead) {
                generateRings(null, null, ringLead - ahead);
            }

            // Clean up old rings
            cleanOldRings();
        }

        // Check deviation — if player flew too far off course
        double deviation = nextRing.center.distance(loc);
        if (deviation > maxDeviation) {
            triggerFall("flew off course");
        }
    }

    private void onRingPassed(ParkourPlayer pp, ElytraRing ring, boolean centered) {
        score++;
        if (startTime == null) startTime = Instant.now();

        new ParkourScoreEvent(pp).call();

        // Visual + sound feedback
        pp.player.spawnParticle(Particle.TOTEM, ring.center, 40, 0.3, 0.3, 0.3, 0.15);
        pp.player.playSound(ring.center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, centered ? 1.8f : 1.2f);

        // Update state score so scoreboard works
        state.score = score;
        state.totalScore++;
    }

    private void onRingMissed(ParkourPlayer pp, ElytraRing ring) {
        // Flash red to indicate miss — no score
        pp.player.spawnParticle(Particle.REDSTONE,
                ring.center, 20, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.RED, 2.0f));
        pp.player.playSound(ring.center, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
    }

    // ── Fall / Reset ──────────────────────────────────────────────────────────

    private void checkFall() {
        for (ParkourPlayer pp : getPlayers()) {
            Player p = pp.player;
            double y = p.getLocation().getY();

            // Touched ground or fell too far below spawn
            if (p.isOnGround() || y < spawnY + MIN_Y_OFFSET) {
                triggerFall("touched ground or fell below course");
                return;
            }
        }
    }

    private void triggerFall(String reason) {
        LoParkour.log("ElytraGenerator fall: " + reason);
        new ParkourFallEvent(player).call();

        // Save score to leaderboard if it's a new record
        Leaderboard lb = getMode().getLeaderboard();
        if (lb != null) {
            int record = lb.get(player.getUUID()).score();
            if (score > record) {
                String time = getFormattedTime();
                getPlayers().forEach(p -> lb.put(p.getUUID(),
                        new Score(p.getName(), time, "0.0", score)));
            }
        }

        // Show fall message
        sendFallMessage();

        reset(true);
    }

    private void sendFallMessage() {
        Leaderboard lb = getMode().getLeaderboard();
        int record = lb != null ? lb.get(player.getUUID()).score() : 0;

        for (ParkourPlayer pp : getPlayers()) {
            pp.sendTranslated("settings.parkour_settings.items.fall_message.divider");
            pp.sendTranslated("settings.parkour_settings.items.fall_message.score", Integer.toString(score));
            pp.sendTranslated("settings.parkour_settings.items.fall_message.time", getFormattedTime());
            pp.sendTranslated("settings.parkour_settings.items.fall_message.high_score", Integer.toString(record));
            if (score > record) {
                pp.sendTranslated("settings.parkour_settings.items.fall_message.formats.beat",
                        Integer.toString(score - record));
            } else if (score == record) {
                pp.sendTranslated("settings.parkour_settings.items.fall_message.formats.tied", "0");
            } else {
                pp.sendTranslated("settings.parkour_settings.items.fall_message.formats.miss",
                        Integer.toString(record - score));
            }
            pp.sendTranslated("settings.parkour_settings.items.fall_message.divider");
        }
    }

    @Override
    public void reset(boolean regenerate) {
        state.stopped = !regenerate;

        // Clear rings from world (remove barrier blocks)
        rings.forEach(ElytraRing::remove);
        rings.clear();
        nextRingIndex = 0;
        score = 0;
        state.score = 0;
        startTime = null;
        state.start = null;
        tickCounter = 0;
        fireworkCooldowns.clear();

        if (!regenerate) {
            stopTasks();
            unregisterListener();
            island.destroy();
            if (getPlayers().isEmpty()) {
                getSpectators().forEach(s -> Modes.DEFAULT.create(s.player));
            }
            return;
        }

        // Teleport back to spawn and restart
        for (ParkourPlayer pp : getPlayers()) {
            pp.teleport(state.playerSpawn);
            pp.player.setGliding(false);
            pp.player.setVelocity(new Vector(0, 0, 0));

            // Restore fireworks
            int amount = Config.CONFIG.getInt("modes.elytra.starting-fireworks");
            pp.player.getInventory().clear();
            pp.player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
            pp.player.getInventory().setItem(0, makeFirework(amount));
            pp.player.setHeldItemSlot(0);
        }

        // Re-generate rings
        Vector initialHeading = state.heading.clone().normalize();
        Location ringOrigin = state.playerSpawn.clone()
                .add(initialHeading.clone().multiply(10)).add(0, 2, 0);
        generateRings(ringOrigin, initialHeading, ringLead);

        // Brief delay then launch
        Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
            for (ParkourPlayer pp : getPlayers()) {
                pp.player.setVelocity(initialHeading.clone().multiply(1.2).setY(0.4));
                pp.player.setGliding(true);
            }
        }, 10L);
    }

    // ── Ring rendering ────────────────────────────────────────────────────────

    private void renderRings() {
        if (state.stopped) return;

        Location playerLoc = player.getLocation();

        for (int i = nextRingIndex; i < Math.min(nextRingIndex + ringLead, rings.size()); i++) {
            ElytraRing ring = rings.get(i);
            double dist = ring.center.distance(playerLoc);
            if (dist > 80) continue; // Don't render very far rings

            // Colour gradient: next ring = gold, further = white, collected = skip
            Color color = (i == nextRingIndex) ? Color.YELLOW
                    : (i == nextRingIndex + 1) ? Color.ORANGE
                    : Color.WHITE;

            renderRing(ring, color);
        }
    }

    private void renderRing(ElytraRing ring, Color color) {
        World world = ring.center.getWorld();
        if (world == null) return;

        // Two orthogonal vectors in the ring's plane
        Vector normal = ring.normal.clone().normalize();
        Vector up = findPerpendicular(normal);
        Vector right = normal.clone().crossProduct(up).normalize();

        double r = ring.radius;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f);

        for (int i = 0; i < RING_POINTS; i++) {
            double angle = 2 * Math.PI * i / RING_POINTS;
            double x = r * Math.cos(angle);
            double y = r * Math.sin(angle);

            Location p = ring.center.clone()
                    .add(right.clone().multiply(x))
                    .add(up.clone().multiply(y));

            world.spawnParticle(Particle.REDSTONE, p, PARTICLES_PER_POINT, 0, 0, 0, 0, dust);
        }

        // Inner ring (smaller, brighter) so it's visible
        Particle.DustOptions innerDust = new Particle.DustOptions(Color.WHITE, 0.8f);
        double innerR = r * 0.4;
        for (int i = 0; i < RING_POINTS / 2; i++) {
            double angle = 2 * Math.PI * i / (RING_POINTS / 2);
            double x = innerR * Math.cos(angle);
            double y = innerR * Math.sin(angle);

            Location p = ring.center.clone()
                    .add(right.clone().multiply(x))
                    .add(up.clone().multiply(y));

            world.spawnParticle(Particle.REDSTONE, p, PARTICLES_PER_POINT, 0, 0, 0, 0, innerDust);
        }
    }

    // ── Firework event — cancel if on cooldown ────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onFireworkUse(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ParkourPlayer pp = ParkourPlayer.getPlayer(p);
        if (pp == null || !(pp.session.generator instanceof ElytraGenerator gen)) return;
        if (gen != this) return;

        // Only care about firework rockets
        org.bukkit.inventory.ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) return;

        boolean isRightClick = event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
        if (!isRightClick) return;
        if (!p.isGliding()) return;

        long now = System.currentTimeMillis();
        int cooldownMs = Config.CONFIG.getInt("modes.elytra.firework-cooldown-ms");
        UUID uuid = p.getUniqueId();

        Long lastUse = fireworkCooldowns.get(uuid);
        if (lastUse != null && now - lastUse < cooldownMs) {
            event.setCancelled(true);
            long remaining = (cooldownMs - (now - lastUse) + 999) / 1000;
            p.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                    "§c⏱ Firework cooldown: §f" + remaining + "s"));
            return;
        }

        fireworkCooldowns.put(uuid, now);

        // Spawn a boost firework at player location instead of vanilla behaviour
        event.setCancelled(true);
        spawnBoostFirework(p);
    }

    /**
     * Spawns a visual-only firework that gives the player a velocity boost.
     * We do it manually so we control the power and remove the damage.
     */
    private void spawnBoostFirework(Player p) {
        double power = Config.CONFIG.getDouble("modes.elytra.boost-power");
        Vector dir = p.getLocation().getDirection().normalize();

        // Remove one firework from inventory
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held.getType() == Material.FIREWORK_ROCKET) {
            if (held.getAmount() > 1) {
                held.setAmount(held.getAmount() - 1);
            } else {
                p.getInventory().setItemInMainHand(null);
            }
        }

        // Apply boost velocity
        Vector boost = dir.multiply(power);
        p.setVelocity(p.getVelocity().add(boost));
        p.setGliding(true);

        // Spawn visual firework that instantly explodes (no damage)
        Firework fw = p.getWorld().spawn(p.getLocation(), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.STAR)
                .withColor(Color.AQUA, Color.WHITE)
                .withFade(Color.YELLOW)
                .withTrail()
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        fw.setShotAtAngle(true);

        // Detonate immediately (no damage tick)
        Scheduler.get(LoParkour.getPlugin()).runLater(() -> {
            if (fw.isValid()) fw.detonate();
        }, 1L);

        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
    }

    // ── Action bar ────────────────────────────────────────────────────────────

    private void updateActionBar(ParkourPlayer pp) {
        if (rings.isEmpty() || nextRingIndex >= rings.size()) return;

        ElytraRing nextRing = rings.get(nextRingIndex);
        double dist = Math.round(nextRing.center.distance(pp.getLocation()) * 10.0) / 10.0;

        // Firework cooldown indicator
        long now = System.currentTimeMillis();
        int cooldownMs = Config.CONFIG.getInt("modes.elytra.firework-cooldown-ms");
        Long lastUse = fireworkCooldowns.get(pp.getUUID());
        String cooldownStr;
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse) + 999) / 1000;
            cooldownStr = "§c⏱ " + remaining + "s";
        } else {
            cooldownStr = "§a✔ Ready";
        }

        pp.player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                "§6⬤ §fRings: §e" + score
                + "  §7|  §f➤ §b" + dist + "m"
                + "  §7|  §f🚀 " + cooldownStr));
    }

    // ── Time helpers ──────────────────────────────────────────────────────────

    private String getFormattedTime() {
        if (startTime == null) return "00:00:000";
        var ms = Instant.now().minusMillis(startTime.toEpochMilli());
        try {
            return DateTimeFormatter.ofPattern("mm:ss:SSS").withZone(ZoneOffset.UTC).format(ms);
        } catch (Exception e) {
            return "00:00:000";
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private void cleanOldRings() {
        // Remove rings more than RING_TRAIL behind the player
        int removeUntil = nextRingIndex - RING_TRAIL;
        if (removeUntil <= 0) return;

        Iterator<ElytraRing> it = rings.iterator();
        int idx = 0;
        while (it.hasNext()) {
            ElytraRing ring = it.next();
            if (idx < removeUntil) {
                ring.remove();
                it.remove();
            } else {
                break;
            }
            idx++;
        }
        nextRingIndex -= removeUntil;
    }

    private void stopTasks() {
        if (tickTask != null)   { tickTask.cancel();   tickTask = null; }
        if (renderTask != null) { renderTask.cancel(); renderTask = null; }
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Returns a vector perpendicular to the given one.
     * Used to form the ring's local coordinate axes.
     */
    private static Vector findPerpendicular(Vector v) {
        Vector perp = (Math.abs(v.getX()) < 0.9)
                ? new Vector(1, 0, 0)
                : new Vector(0, 1, 0);
        return perp.subtract(v.clone().multiply(perp.dot(v))).normalize();
    }

    /**
     * Rotates a vector by yaw (horizontal) and pitch (vertical) deltas.
     */
    private static Vector rotateVector(Vector v, double yawRad, double pitchRad) {
        // Rotate around Y axis (yaw)
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double nx = v.getX() * cos - v.getZ() * sin;
        double nz = v.getX() * sin + v.getZ() * cos;
        v = new Vector(nx, v.getY(), nz);

        // Rotate around the local right axis (pitch)
        Vector right = new Vector(v.getZ(), 0, -v.getX()).normalize();
        double c = Math.cos(pitchRad);
        double s = Math.sin(pitchRad);
        double vDotR = v.dot(right);
        return new Vector(
            v.getX() * c + (right.getY() * v.getZ() - right.getZ() * v.getY()) * s,
            v.getY() * c + (right.getZ() * v.getX() - right.getX() * v.getZ()) * s,
            v.getZ() * c + (right.getX() * v.getY() - right.getY() * v.getX()) * s
        );
    }

    private static ItemStack makeFirework(int amount) {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET, amount);
        FireworkMeta meta = (FireworkMeta) item.getItemMeta();
        if (meta != null) {
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL)
                    .withColor(Color.AQUA)
                    .build());
            meta.setPower(1);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Inner class: ElytraRing ───────────────────────────────────────────────

    /**
     * Represents one ring in the course.
     *
     * <p>A ring is defined by its center point and its normal vector (the direction
     * of travel through it). The "inside" is a circle of {@code radius} around the center
     * on the plane perpendicular to {@code normal}.
     */
    public static class ElytraRing {

        public final int id;
        public final Location center;
        /** Direction of travel — the ring faces perpendicular to this. */
        public final Vector normal;
        public final double radius;

        /** Signed distance of the player on the last tick (for plane crossing detection). */
        private double lastSignedDist = Double.NaN;

        /** Barrier blocks placed for this ring (optional, used if solid rings are enabled). */
        private final List<Block> barrierBlocks = new ArrayList<>();

        public ElytraRing(int id, Location center, Vector normal, double radius) {
            this.id = id;
            this.center = center.clone();
            this.normal = normal.clone().normalize();
            this.radius = radius;
        }

        /**
         * Returns true if the player crossed the ring's plane since the last call.
         * Uses the dot product of (playerPos - ringCenter) with the ring's normal.
         * Sign change = crossing.
         */
        public boolean hasCrossed(Location playerLoc, Vector velocity) {
            Vector toPlayer = playerLoc.toVector().subtract(center.toVector());
            double signedDist = toPlayer.dot(normal);

            boolean crossed = false;
            if (!Double.isNaN(lastSignedDist)) {
                // Crossed if sign flipped (was positive, now negative or vice versa)
                crossed = (lastSignedDist > 0) != (signedDist > 0);
            }
            lastSignedDist = signedDist;
            return crossed;
        }

        /**
         * Returns true if the player's position (projected onto the ring's plane)
         * is within the ring's radius — i.e. they flew through the hole.
         */
        public boolean isInside(Location playerLoc) {
            // Project player position onto ring's plane
            Vector toPlayer = playerLoc.toVector().subtract(center.toVector());
            // Remove the component along the normal
            Vector projected = toPlayer.clone().subtract(normal.clone().multiply(toPlayer.dot(normal)));
            return projected.length() <= radius;
        }

        /** Remove any barrier blocks this ring placed in the world. */
        public void remove() {
            barrierBlocks.forEach(b -> b.setType(Material.AIR, false));
            barrierBlocks.clear();
        }
    }
}
