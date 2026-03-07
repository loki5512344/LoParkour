package dev.loki.loparkour.mode;

import dev.loki.loparkour.util.Item;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GravityShiftMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);
    private final Random random = new Random();

    @Override
    @NotNull
    public String getName() {
        return "gravity-shift";
    }

    @Override
    @Nullable
    public dev.loki.loparkour.util.Item getItem(String locale) {
        return Locales.getItem(locale, "play.single.gravity-shift");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage("Joining is currently disabled.");
            return;
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator != null && pp.session.generator.getMode() instanceof GravityShiftMode) {
            return;
        }

        player.closeInventory();

        Session.create(session -> new GravityShiftGenerator(session), null, null, player);
    }

    private static class GravityShiftGenerator extends ParkourGenerator {
        private int jumpsUntilShift;
        private final int shiftInterval;
        private PotionEffect currentEffect;

        public GravityShiftGenerator(@NotNull Session session) {
            super(session);
            this.shiftInterval = Config.CONFIG.getInt("modes.gravity-shift.interval");
            this.jumpsUntilShift = shiftInterval;
        }

        @Override
        protected void score() {
            super.score();
            
            jumpsUntilShift--;
            
            if (jumpsUntilShift <= 0) {
                applyRandomEffect();
                jumpsUntilShift = shiftInterval;
            }
        }

        private void applyRandomEffect() {
            if (currentEffect != null) {
                player.player.removePotionEffect(currentEffect.getType());
            }

            List<EffectData> effects = new ArrayList<>();
            
            if (Config.CONFIG.getBoolean("modes.gravity-shift.effects.jump-boost.enabled")) {
                effects.add(new EffectData(
                    PotionEffectType.JUMP,
                    Config.CONFIG.getInt("modes.gravity-shift.effects.jump-boost.amplifier"),
                    Config.CONFIG.getInt("modes.gravity-shift.effects.jump-boost.duration"),
                    "Jump Boost"
                ));
            }
            
            if (Config.CONFIG.getBoolean("modes.gravity-shift.effects.speed.enabled")) {
                effects.add(new EffectData(
                    PotionEffectType.SPEED,
                    Config.CONFIG.getInt("modes.gravity-shift.effects.speed.amplifier"),
                    Config.CONFIG.getInt("modes.gravity-shift.effects.speed.duration"),
                    "Speed"
                ));
            }
            
            if (Config.CONFIG.getBoolean("modes.gravity-shift.effects.slowness.enabled")) {
                effects.add(new EffectData(
                    PotionEffectType.SLOW,
                    Config.CONFIG.getInt("modes.gravity-shift.effects.slowness.amplifier"),
                    Config.CONFIG.getInt("modes.gravity-shift.effects.slowness.duration"),
                    "Slowness"
                ));
            }
            
            if (Config.CONFIG.getBoolean("modes.gravity-shift.effects.levitation.enabled")) {
                effects.add(new EffectData(
                    PotionEffectType.LEVITATION,
                    Config.CONFIG.getInt("modes.gravity-shift.effects.levitation.amplifier"),
                    Config.CONFIG.getInt("modes.gravity-shift.effects.levitation.duration"),
                    "Levitation"
                ));
            }

            if (effects.isEmpty()) {
                return;
            }

            EffectData selected = effects.get(new Random().nextInt(effects.size()));
            currentEffect = new PotionEffect(
                selected.type,
                selected.duration * 20,
                selected.amplifier - 1,
                false,
                true,
                true
            );

            player.player.addPotionEffect(currentEffect);
            
            // Visual feedback
            player.player.sendTitle("§6⚡", "§e" + selected.name, 5, 20, 10);
            player.player.playSound(player.player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
            player.player.spawnParticle(org.bukkit.Particle.PORTAL, player.player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.5);
            
            player.sendTranslated("modes.gravity-shift.effect-applied", selected.name);
        }

        @Override
        public void reset(boolean regenerate) {
            if (currentEffect != null && player != null && player.player != null) {
                player.player.removePotionEffect(currentEffect.getType());
            }
            jumpsUntilShift = shiftInterval;
            super.reset(regenerate);
        }

        @Override
        public Mode getMode() {
            return Modes.GRAVITY_SHIFT;
        }
    }

    private static class EffectData {
        final PotionEffectType type;
        final int amplifier;
        final int duration;
        final String name;

        EffectData(PotionEffectType type, int amplifier, int duration, String name) {
            this.type = type;
            this.amplifier = amplifier;
            this.duration = duration;
            this.name = name;
        }
    }
}
