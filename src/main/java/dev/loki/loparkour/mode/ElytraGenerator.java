package dev.loki.loparkour.mode;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.mode.elytra.*;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.util.ColorUtil;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Refactored Elytra mode generator using composition pattern.
 * Coordinates ring generation, physics, and rendering components.
 */
public class ElytraGenerator extends ParkourGenerator implements Listener {

    private final ElytraConfig config;
    private final ElytraRingGenerator ringGenerator;
    private final ElytraPhysics physics;
    private final ElytraRenderer renderer;
    
    private final List<ElytraRing> rings = new ArrayList<>();
    private final Map<ParkourPlayer, Integer> playerRingIndex = new HashMap<>();
    
    private BukkitTask renderTask;
    private BukkitTask physicsTask;
    
    public ElytraGenerator(@NotNull Session session) {
        super(session);
        
        this.config = new ElytraConfig();
        this.ringGenerator = new ElytraRingGenerator(config);
        this.physics = new ElytraPhysics(config);
        this.renderer = new ElytraRenderer();
        
        Bukkit.getPluginManager().registerEvents(this, LoParkour.getPlugin());
    }
    
    @Override
    public void generateFirst(Location spawn, Location blockSpawn) {
        // Generate initial rings starting from spawn position
        Vector initialDirection = new Vector(1, 0, 0); // East direction
        List<ElytraRing> initialRings = ringGenerator.generateRings(spawn, initialDirection, config.getRingLead());
        rings.addAll(initialRings);

        // Initialize player data and launch them
        for (ParkourPlayer player : getPlayers()) {
            playerRingIndex.put(player, 0);
            giveElytraAndFireworks(player);
            launchPlayer(player, spawn);
        }

        startTasks();
    }

    private void launchPlayer(@NotNull ParkourPlayer player, @NotNull Location spawn) {
        Player p = player.player;

        // Teleport player above spawn (10 blocks up)
        Location launchPos = spawn.clone().add(0, 10, 0);
        p.teleport(launchPos);

        // Enable gliding
        p.setGliding(true);

        // Give initial forward velocity
        Vector velocity = new Vector(1, 0, 0).normalize().multiply(1.5);
        p.setVelocity(velocity);
    }
    
    @Override
    public void tick() {
        super.tick();
        checkRingsAndPhysics();
        generateMoreRings();
        cleanupOldRings();
    }
    
    @Override
    public void reset(boolean regenerate) {
        cleanup();  // Call cleanup BEFORE super.reset to cancel tasks first
        super.reset(regenerate);
    }
    
    private void checkRingsAndPhysics() {
        for (ParkourPlayer player : getPlayers()) {
            int currentIndex = playerRingIndex.getOrDefault(player, 0);
            
            // Check ring crossings
            int newIndex = physics.checkRingCrossings(player, rings, currentIndex);
            if (newIndex > currentIndex) {
                onRingPassed(player, rings.get(newIndex - 1));
                playerRingIndex.put(player, newIndex);
            }
            
            // Check for falls
            ElytraPhysics.FallCheckResult fallResult = physics.checkFall(player, rings, currentIndex);
            if (fallResult.shouldFall()) {
                triggerFall(player, fallResult.getReason());
            }
            
            updateActionBar(player);
        }
    }
    
    private void generateMoreRings() {
        if (rings.isEmpty()) {
            return;
        }
        if (rings.size() < config.getRingLead() * 2) {
            ElytraRing lastRing = rings.get(rings.size() - 1);
            Vector direction = calculateNextDirection(lastRing);
            
            List<ElytraRing> newRings = ringGenerator.generateRings(
                lastRing.getCenter(), direction, config.getRingLead()
            );
            rings.addAll(newRings);
        }
    }
    
    private void onRingPassed(@NotNull ParkourPlayer player, @NotNull ElytraRing ring) {
        boolean centered = physics.isPlayerCentered(player.player.getLocation(), ring);
        int points = centered ? 10 : 5;
        
        player.session.generator.state.score += points;
        
        // Play sound and show message
        player.player.playSound(player.player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        String message = centered ? "Perfect! +" + points : "Good! +" + points;
        player.player.sendMessage(ColorUtil.color("&a" + message));
    }
    
    private void triggerFall(@NotNull ParkourPlayer player, @NotNull String reason) {
        fall();
        player.player.sendMessage(ColorUtil.color("&cFell: " + reason));
    }
    
    @SuppressWarnings("deprecation") // Spigot Chat API: action bar via TextComponent
    private void updateActionBar(@NotNull ParkourPlayer player) {
        int currentIndex = playerRingIndex.getOrDefault(player, 0);
        int totalRings = rings.size();
        int score = state.score;
        
        String message = String.format("&eRing: %d/%d &7| &aScore: %d", 
            currentIndex + 1, totalRings, score);
        
        player.player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtil.color(message))
        );
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = findParkourPlayer(player);
        if (pp == null) return;
        
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
            if (physics.applyFireworkBoost(pp)) {
                spawnBoostFirework(player);
                item.setAmount(item.getAmount() - 1);
            }
            event.setCancelled(true);
        }
    }
    
    private void startTasks() {
        renderTask = Bukkit.getScheduler().runTaskTimer(LoParkour.getPlugin(), () -> {
            for (ParkourPlayer player : getPlayers()) {
                int currentIndex = playerRingIndex.getOrDefault(player, 0);
                renderer.renderRings(rings, currentIndex);
            }
        }, 0L, 2L); // Every 2 ticks
    }
    
    private void cleanup() {
        // Cancel and nullify tasks to prevent memory leaks
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }
        if (physicsTask != null) {
            physicsTask.cancel();
            physicsTask = null;
        }
        
        // Unregister event listener to prevent memory leaks
        HandlerList.unregisterAll(this);
        
        // Clear all data structures
        rings.forEach(ElytraRing::remove);
        rings.clear();
        playerRingIndex.clear();
        
        for (ParkourPlayer player : getPlayers()) {
            physics.cleanup(player.player.getUniqueId());
        }
    }
    
    private void giveElytraAndFireworks(@NotNull ParkourPlayer player) {
        Player p = player.player;
        p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        p.getInventory().addItem(makeFirework(config.getStartingFireworks()));
    }
    
    private void spawnBoostFirework(@NotNull Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }
    
    @NotNull
    private Vector calculateNextDirection(@NotNull ElytraRing lastRing) {
        // Simple forward direction for now
        return new Vector(1, 0, 0);
    }
    
    private void cleanupOldRings() {
        // Remove rings that are far behind all players
        // Implementation depends on specific requirements
    }
    
    @NotNull
    private ItemStack makeFirework(int amount) {
        ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET, amount);
        FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
        if (meta != null) {
            meta.setPower(1);
            firework.setItemMeta(meta);
        }
        return firework;
    }
    
    private ParkourPlayer findParkourPlayer(@NotNull Player player) {
        return getPlayers().stream()
            .filter(pp -> pp.player.equals(player))
            .findFirst()
            .orElse(null);
    }
}