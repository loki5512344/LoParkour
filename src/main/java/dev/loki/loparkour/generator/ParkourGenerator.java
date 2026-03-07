package dev.loki.loparkour.generator;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.world.Divider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ParkourGenerator {

    public static final int BLOCK_TRAIL = 2;

    public final GeneratorState state = new GeneratorState();
    public final GeneratorLifecycle lifecycle;
    public final EffectManager effects;
    public final BlockPlacer placer;

    public final Session session;
    public final ParkourPlayer player;
    public final Profile profile = new Profile();
    public final Island island;
    public final List<GeneratorOption> generatorOptions;

    public Location[] zone;
    public dev.lolib.scheduler.ScheduledTask task;

    public ParkourGenerator(@NotNull Session session, @Nullable LPSchematic schematic,
                            GeneratorOption... generatorOptions) {
        this.session = session;
        this.generatorOptions = Arrays.asList(generatorOptions);
        this.player = session.getPlayers().get(0);
        this.island = new Island(session, schematic);
        this.zone = Divider.toSelection(session);
        this.state.schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
        this.state.heading = Option.HEADING.getDirection();
        
        this.effects = new EffectManager(this);
        this.placer = new BlockPlacer(this);
        this.lifecycle = new GeneratorLifecycle(this);
        
        calculateChances();
    }

    public ParkourGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        this(session, loadIslandSchematic(), generatorOptions);
    }

    private static LPSchematic loadIslandSchematic() {
        String name = Config.GENERATION.getString("advanced.island.schematic-name");
        if (name == null || name.isEmpty()) return null;
        var manager = LoParkour.getSchematicManager();
        if (manager == null) return null;
        LPSchematic s = manager.getSchematic(name);
        if (s == null) {
            LoParkour.getPlugin().getLogger().warning("Island schematic '%s' not found!".formatted(name));
        }
        return s;
    }

    public void overrideProfile() {}

    protected void calculateChances() {
        state.defaultChances.clear();
        state.defaultChances.put(BlockGenerationType.DEFAULT, Option.TYPE_NORMAL);
        state.defaultChances.put(BlockGenerationType.SCHEMATIC, Option.TYPE_SCHEMATICS);
        state.defaultChances.put(BlockGenerationType.SPECIAL, Option.TYPE_SPECIAL);

        state.heightChances.clear();
        state.heightChances.put(1, Option.NORMAL_HEIGHT_1);
        state.heightChances.put(0, Option.NORMAL_HEIGHT_0);
        state.heightChances.put(-1, Option.NORMAL_HEIGHT_NEG1);
        state.heightChances.put(-2, Option.NORMAL_HEIGHT_NEG2);

        state.distanceChances.clear();
        state.distanceChances.put(1, Option.NORMAL_DISTANCE_1);
        state.distanceChances.put(2, Option.NORMAL_DISTANCE_2);
        state.distanceChances.put(3, Option.NORMAL_DISTANCE_3);
        state.distanceChances.put(4, Option.NORMAL_DISTANCE_4);

        state.specialChances.clear();
        state.specialChances.put(Material.PACKED_ICE.createBlockData(), Option.SPECIAL_ICE);
        state.specialChances.put(Material.SMOOTH_QUARTZ_SLAB.createBlockData("[type=bottom]"), Option.SPECIAL_SLAB);
        state.specialChances.put(Material.GLASS_PANE.createBlockData(), Option.SPECIAL_PANE);
        state.specialChances.put(Material.OAK_FENCE.createBlockData(), Option.SPECIAL_FENCE);
    }

    public void generate() { placer.generate(); }
    public void generate(int amount) { placer.generate(amount); }
    public void generateFirst(Location spawn, Location block) { placer.generateFirst(spawn, block); }

    public void startTick() { lifecycle.startTick(); }
    public void tick() { lifecycle.tick(); }
    protected void fall() { lifecycle.fall(); }
    protected void score() { lifecycle.score(); }

    public void reset(boolean regenerate) {
        state.stopped = !regenerate;
        if (!regenerate && task == null) {
            LoParkour.getPlugin().getLogger().warning("## Incomplete joining setup — report this!");
        }
        state.lastPositionIndexPlayer = 0;
        if (!state.history.isEmpty()) {
            state.history.remove(0);
            state.history.forEach(b -> b.setType(Material.AIR, false));
            state.history.clear();
        }
        state.resetSchematicState();
        placer.deleteSchematic();
        Leaderboard lb = getMode().getLeaderboard();
        int record = lb != null ? lb.get(player.getUUID()).score() : 0;
        if (profile.get("showFallMessage").asBoolean()) {
            sendFallMessage(record);
        }
        if (lb != null && state.score > record) {
            registerScore(getDetailedTime(), Double.toString(getDifficultyScore()).substring(0, 3), state.score);
        }
        state.resetScore();
        state.heading = Option.HEADING.getDirection();
        if (regenerate) {
            player.teleport(state.playerSpawn);
            generateFirst(state.playerSpawn, state.blockSpawn);
            return;
        }
        island.destroy();
        if (getPlayers().isEmpty()) {
            getSpectators().forEach(s -> Modes.DEFAULT.create(s.player));
        }
    }

    private void sendFallMessage(int record) {
        String key;
        int number = 0;
        if (state.score == record) {
            key = "settings.parkour_settings.items.fall_message.formats.tied";
        } else if (state.score > record) {
            key = "settings.parkour_settings.items.fall_message.formats.beat";
            number = state.score - record;
        } else {
            key = "settings.parkour_settings.items.fall_message.formats.miss";
            number = record - state.score;
        }
        for (ParkourPlayer p : getPlayers()) {
            p.sendTranslated("settings.parkour_settings.items.fall_message.divider");
            p.sendTranslated("settings.parkour_settings.items.fall_message.score", Integer.toString(state.score));
            p.sendTranslated("settings.parkour_settings.items.fall_message.time", getFormattedTime());
            p.sendTranslated("settings.parkour_settings.items.fall_message.high_score", Integer.toString(record));
            p.sendTranslated(key, Integer.toString(number));
            p.sendTranslated("settings.parkour_settings.items.fall_message.divider");
        }
    }

    protected void registerScore(String time, String difficulty, int score) {
        Leaderboard lb = getMode().getLeaderboard();
        if (lb == null) return;
        getPlayers().forEach(p -> lb.put(p.getUUID(), new Score(p.getName(), time, difficulty, score)));
    }

    public void menu(ParkourPlayer player) { Menus.PARKOUR_SETTINGS.open(player); }
    public Block getLatest() { return state.history.get(state.history.size() - 1); }

    public double getDifficultyScore() {
        double s = 0;
        if (profile.get("useSpecialBlocks").asBoolean()) s += 0.5;
        double d = profile.get("schematicDifficulty").asDouble();
        if (d > 0) s += d <= 0.25 ? 0.2 : d <= 0.5 ? 0.3 : d <= 0.75 ? 0.4 : 0.5;
        return s;
    }

    public String getFormattedTime() {
        return getTime(Config.CONFIG.getString("options.time.score-format"));
    }

    public String getDetailedTime() {
        return getTime("mm:ss:SSS");
    }

    private String getTime(String format) {
        var ms = Instant.now().minusMillis(state.start != null ? state.start.toEpochMilli() : Instant.now().toEpochMilli());
        try {
            return DateTimeFormatter.ofPattern(format).withZone(ZoneOffset.UTC).format(ms);
        } catch (IllegalArgumentException ex) {
            LoParkour.getPlugin().getLogger().severe("Invalid time format: " + format);
            return "";
        }
    }

    public Mode getMode() { return Modes.DEFAULT; }
    @NotNull public List<ParkourPlayer> getPlayers() { return session.getPlayers(); }
    @NotNull public List<ParkourSpectator> getSpectators() { return session.getSpectators(); }

    public enum BlockGenerationType { DEFAULT, SCHEMATIC, SPECIAL }
}
