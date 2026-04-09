package dev.loki.loparkour.mode;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.util.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Gravity Shift Mode — random potion effects applied every N jumps.
 */
public class GravityShiftMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "gravity-shift";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return Locales.getItem(locale, "modes.gravity-shift");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage("§cJoining is currently disabled.");
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator instanceof GravityShiftGenerator) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new GravityShiftGenerator(session), null, null, player);
    }

    // ── GravityShiftGenerator ─────────────────────────────────────────────────

    private static class GravityShiftGenerator extends ParkourGenerator {

        private final int interval;
        private final Map<PotionEffectType, EffectConfig> effects;
        private final Random random;
        private final Map<UUID, Integer> jumpCounts;

        public GravityShiftGenerator(@NotNull Session session) {
            super(session);
            this.random = new Random();
            this.jumpCounts = new HashMap<>();

            // Load config
            this.interval = Config.CONFIG.isPath("modes.gravity-shift.interval")
                ? Config.CONFIG.getInt("modes.gravity-shift.interval") : 10;

            this.effects = new HashMap<>();
            loadEffect("jump-boost", PotionEffectType.JUMP);
            loadEffect("speed", PotionEffectType.SPEED);
            loadEffect("slowness", PotionEffectType.SLOW);
            loadEffect("levitation", PotionEffectType.LEVITATION);

            // Initialize jump counts for all players
            for (ParkourPlayer pp : getPlayers()) {
                jumpCounts.put(pp.getUUID(), 0);
            }
        }

        private void loadEffect(String key, PotionEffectType type) {
            String basePath = "modes.gravity-shift.effects." + key;
            boolean enabled = Config.CONFIG.isPath(basePath + ".enabled")
                ? Config.CONFIG.getBoolean(basePath + ".enabled") : false;

            if (!enabled) {
                return;
            }

            int amplifier = Config.CONFIG.isPath(basePath + ".amplifier")
                ? Config.CONFIG.getInt(basePath + ".amplifier") : 1;
            int duration = Config.CONFIG.isPath(basePath + ".duration")
                ? Config.CONFIG.getInt(basePath + ".duration") : 30;

            effects.put(type, new EffectConfig(amplifier, duration * 20));
        }

        @Override
        protected void score() {
            super.score();

            // Apply effects to all players
            for (ParkourPlayer pp : getPlayers()) {
                UUID uuid = pp.getUUID();
                int count = jumpCounts.getOrDefault(uuid, 0) + 1;
                jumpCounts.put(uuid, count);

                if (count % interval == 0 && !effects.isEmpty()) {
                    applyRandomEffect(pp.player);
                }
            }
        }

        private void applyRandomEffect(Player player) {
            if (player == null || effects.isEmpty()) {
                return;
            }

            List<PotionEffectType> types = new ArrayList<>(effects.keySet());
            PotionEffectType selected = types.get(random.nextInt(types.size()));
            EffectConfig config = effects.get(selected);

            player.removePotionEffect(selected);
            player.addPotionEffect(new PotionEffect(
                selected,
                config.durationTicks,
                config.amplifier,
                false,
                true,
                true
            ));
        }

        @Override
        public void reset(boolean regenerate) {
            // Clear effects and reset counts
            for (ParkourPlayer pp : getPlayers()) {
                Player player = pp.player;
                if (player != null) {
                    for (PotionEffectType type : effects.keySet()) {
                        player.removePotionEffect(type);
                    }
                }
                jumpCounts.put(pp.getUUID(), 0);
            }

            super.reset(regenerate);
        }

        @Override
        public Mode getMode() {
            return Registry.getMode("gravity-shift");
        }

        private record EffectConfig(int amplifier, int durationTicks) {}
    }
}
