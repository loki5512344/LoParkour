package dev.loki.loparkour.mode;

import dev.loki.loparkour.util.Item;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.ghost.GhostManager;
import dev.loki.loparkour.ghost.GhostRecorder;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.session.Session;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * Elytra Parkour Mode - Flying parkour with rings and boost
 */
public class ElytraMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "elytra";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return Locales.getItem(locale, "modes.elytra");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage(Locales.getString(player, "other.joining_disabled"));
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof ElytraMode) {
            return;
        }

        player.closeInventory();

        // Create session with elytra setup
        Session.create(session -> new ElytraGenerator(session), null, null, player);
    }

    private static class ElytraGenerator extends ParkourGenerator {
        private final List<RingCheckpoint> rings = new ArrayList<>();
        private final Map<UUID, Long> fireworkCooldowns = new HashMap<>();
        private final Map<UUID, Integer> ringsCollected = new HashMap<>();
        private int tickCounter = 0;

        public ElytraGenerator(@NotNull Session session) {
            super(session);
            
            // Give elytra and fireworks to all players in session
            for (ParkourPlayer parkourPlayer : session.getPlayers()) {
                Player p = parkourPlayer.player;
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
                
                int fireworks = Config.CONFIG.getInt("modes.elytra.starting-fireworks");
                p.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, fireworks));
                
                ringsCollected.put(p.getUniqueId(), 0);
                
                // Notify player
                p.sendTitle("§b§lELYTRA MODE", "§7Fly through rings for bonus points!", 10, 70, 20);
                p.sendMessage("§b§lElytra Mode §7activated! Use fireworks to boost.");
            }
        }

        @Override
        public void tick() {
            // Don't call super.tick() - we don't want normal block generation
            tickCounter++;
            
            // Update spectators
            getSpectators().forEach(ParkourSpectator::update);
            
            // Check for fall (much lower threshold for elytra)
            if (player.getLocation().getY() < state.lastStandingPlayerLocation.getY() - 50) {
                lifecycle.fall();
                return;
            }
            
            // Spawn rings every 2 seconds
            if (tickCounter % 40 == 0) {
                spawnNextRing();
            }
            
            // Display rings and check for player passing through
            Iterator<RingCheckpoint> it = rings.iterator();
            while (it.hasNext()) {
                RingCheckpoint ring = it.next();
                
                // Display ring particles
                displayRing(ring.location);
                
                // Check if any player passed through
                for (ParkourPlayer pp : getPlayers()) {
                    if (ring.collected) continue;
                    
                    BoundingBox playerBox = pp.player.getBoundingBox();
                    if (ring.getBoundingBox().overlaps(playerBox)) {
                        ring.collected = true;
                        int count = ringsCollected.getOrDefault(pp.getUUID(), 0) + 1;
                        ringsCollected.put(pp.getUUID(), count);
                        
                        // Bonus points for passing through ring
                        lifecycle.score();
                        lifecycle.score();
                        lifecycle.score(); // 3 points per ring
                        
                        pp.player.playSound(pp.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                        pp.player.spawnParticle(Particle.TOTEM, ring.location, 20, 0.5, 0.5, 0.5, 0.1);
                        
                        // Update last standing location to ring location
                        state.lastStandingPlayerLocation = ring.location.clone();
                    }
                }
                
                // Remove old rings
                if (ring.location.distance(player.getLocation()) > 100) {
                    it.remove();
                }
            }
            
            // Check firework cooldown
            checkFireworkCooldown();
            
            // Start timer if not started
            if (state.start == null) state.start = Instant.now();
        }
        
        private void spawnNextRing() {
            Location playerLoc = player.getLocation();
            
            // Spawn ring ahead of player in flight direction
            Vector direction = player.getLocation().getDirection();
            Location ringLoc = playerLoc.clone().add(direction.multiply(20)).add(0, 5, 0);
            
            // Add some randomness
            ringLoc.add((Math.random() - 0.5) * 10, (Math.random() - 0.5) * 5, (Math.random() - 0.5) * 10);
            
            rings.add(new RingCheckpoint(ringLoc));
        }
        
        private void displayRing(Location center) {
            World world = center.getWorld();
            if (world == null) return;
            
            double radius = 2.0;
            int points = 30;
            
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                
                Location particleLoc = new Location(world, x, center.getY(), z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
        }
        
        private void checkFireworkCooldown() {
            long now = System.currentTimeMillis();
            int cooldownMs = Config.CONFIG.getInt("modes.elytra.firework-cooldown-ms");
            
            for (ParkourPlayer pp : getPlayers()) {
                UUID uuid = pp.getUUID();
                ItemStack mainHand = pp.player.getInventory().getItemInMainHand();
                ItemStack offHand = pp.player.getInventory().getItemInOffHand();
                
                boolean usingFirework = (mainHand.getType() == Material.FIREWORK_ROCKET && pp.player.isGliding())
                                     || (offHand.getType() == Material.FIREWORK_ROCKET && pp.player.isGliding());
                
                if (usingFirework) {
                    Long lastUse = fireworkCooldowns.get(uuid);
                    if (lastUse != null && now - lastUse < cooldownMs) {
                        long remaining = (cooldownMs - (now - lastUse)) / 1000;
                        pp.player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§cFirework cooldown: " + remaining + "s"));
                        continue;
                    }
                    fireworkCooldowns.put(uuid, now);
                }
            }
        }

        @Override
        public Mode getMode() {
            return Modes.ELYTRA;
        }
        
        private static class RingCheckpoint {
            final Location location;
            boolean collected = false;
            
            RingCheckpoint(Location location) {
                this.location = location;
            }
            
            BoundingBox getBoundingBox() {
                return BoundingBox.of(location, 2.5, 2.5, 2.5);
            }
        }
    }
}
