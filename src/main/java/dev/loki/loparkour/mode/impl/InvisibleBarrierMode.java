package dev.loki.loparkour.mode.impl;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.generator.core.coordinator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.core.Leaderboard;
import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.mode.base.ModeMessages;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.session.core.Session;
import dev.loki.loparkour.util.item.Item;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class InvisibleBarrierMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "invisible-barrier";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.invisible-barrier");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!ModeMessages.checkJoiningEnabled(player)) {
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator instanceof BarrierGenerator) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new BarrierGenerator(session), null, null, player);
    }

    static class BarrierGenerator extends ParkourGenerator {

        private static final double RENDER_DISTANCE = 24.0;
        private static final double PARTICLE_STEP = 0.1;

        private final Set<Location> activeBarriers = new HashSet<>();
        private final Random random = new Random();
        private final Map<Location, Particle.DustOptions> blockColors = new HashMap<>();
        private int particleTick = 0;

        public BarrierGenerator(@NotNull Session session) {
            super(session);
        }

        @Override
        public void generateFirst(@NotNull Location spawn, @NotNull Location block) {
            super.generateFirst(spawn, block);
            convertLastBlockToBarrier();
        }

        @Override
        public void generate(int amount) {
            super.generate(amount);
            int start = Math.max(0, state.history.size() - amount);
            for (int i = start; i < state.history.size(); i++) {
                convertBlockToBarrier(state.history.get(i));
            }
        }

        @Override
        public void tick() {
            super.tick();
            particleTick++;
            if (particleTick % 4 == 0) {
                renderOutlines();
            }
        }

        @Override
        public void reset(boolean regenerate) {
            activeBarriers.clear();
            blockColors.clear();
            particleTick = 0;
            super.reset(regenerate);
        }

        @Override
        public Mode getMode() {
            return Modes.INVISIBLE_BARRIER;
        }

        private void convertLastBlockToBarrier() {
            if (state.history.isEmpty()) return;
            convertBlockToBarrier(state.history.get(state.history.size() - 1));
        }

        private void convertBlockToBarrier(@NotNull Block block) {
            Location loc = block.getLocation();
            if (block.getType() == Material.BARRIER) return;
            block.setType(Material.BARRIER, false);
            activeBarriers.add(loc);
            Color color = Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            blockColors.put(loc, new Particle.DustOptions(color, 1.0f));
        }

        private void renderOutlines() {
            if (activeBarriers.isEmpty()) return;

            // Self-clean invalid entries
            Iterator<Map.Entry<Location, Particle.DustOptions>> it = blockColors.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Location, Particle.DustOptions> entry = it.next();
                if (entry.getKey().getBlock().getType() != Material.BARRIER) {
                    it.remove();
                    activeBarriers.remove(entry.getKey());
                }
            }

            for (ParkourPlayer pp : getPlayers()) {
                Player player = pp.player;
                Location playerLoc = player.getLocation();

                for (Location loc : activeBarriers) {
                    if (loc.getWorld() != playerLoc.getWorld()) continue;
                    if (loc.distanceSquared(playerLoc) > RENDER_DISTANCE * RENDER_DISTANCE) continue;

                    Particle.DustOptions options = blockColors.get(loc);
                    if (options == null) continue;

                    drawBlockOutline(player, loc, options);
                }
            }
        }

        private void drawBlockOutline(@NotNull Player player, @NotNull Location loc, @NotNull Particle.DustOptions options) {
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();

            // Bottom edges (y offset = 0)
            drawEdge(player, x, y, z, x + 1, y, z, options);
            drawEdge(player, x + 1, y, z, x + 1, y, z + 1, options);
            drawEdge(player, x + 1, y, z + 1, x, y, z + 1, options);
            drawEdge(player, x, y, z + 1, x, y, z, options);

            // Top edges (y offset = 1)
            drawEdge(player, x, y + 1, z, x + 1, y + 1, z, options);
            drawEdge(player, x + 1, y + 1, z, x + 1, y + 1, z + 1, options);
            drawEdge(player, x + 1, y + 1, z + 1, x, y + 1, z + 1, options);
            drawEdge(player, x, y + 1, z + 1, x, y + 1, z, options);

            // Vertical edges
            drawEdge(player, x, y, z, x, y + 1, z, options);
            drawEdge(player, x + 1, y, z, x + 1, y + 1, z, options);
            drawEdge(player, x + 1, y, z + 1, x + 1, y + 1, z + 1, options);
            drawEdge(player, x, y, z + 1, x, y + 1, z + 1, options);
        }

        private void drawEdge(@NotNull Player player, double x1, double y1, double z1, double x2, double y2, double z2, @NotNull Particle.DustOptions options) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int steps = Math.max(1, (int) (length / PARTICLE_STEP));

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double px = x1 + dx * t;
                double py = y1 + dy * t;
                double pz = z1 + dz * t;
                player.spawnParticle(Particle.REDSTONE, px, py, pz, 1, options);
            }
        }
    }
}
