package dev.loki.loparkour.generator;

import dev.lolib.scheduler.Scheduler;
import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.api.event.ParkourFallEvent;
import dev.loki.loparkour.api.event.ParkourScoreEvent;
import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.leaderboard.Score;
import dev.loki.loparkour.menu.Menus;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.player.ParkourSpectator;
import dev.loki.loparkour.reward.Rewards;
import dev.loki.loparkour.schematic.lpschem.LPSchematic;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.world.Divider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Coordinates parkour generation, ticking, scoring, and reset.
 *
 * Heavy lifting is delegated to:
 * <ul>
 *   <li>{@link BlockPlacer}   – block selection &amp; placement</li>
 *   <li>{@link EffectManager} – particles &amp; sounds</li>
 * </ul>
 */
public class ParkourGenerator {

    public static final int BLOCK_TRAIL = 2;

    // ── State ──────────────────────────────────────────────────────────────────
    public int score = 0;
    public int totalScore = 0;
    public int schematicCooldown;
    public boolean stopped = false;

    public Location[] zone;
    public ParkourPlayer player;
    public dev.lolib.scheduler.ScheduledTask task;
    private dev.lolib.scheduler.ScheduledTask cleanupTask;

    public Location blockSpawn;
    public Location playerSpawn;
    public Instant start;
    public Vector heading = Option.HEADING.getDirection();

    public final List<GeneratorOption> generatorOptions;
    public final Session session;
    public final Profile profile = new Profile();
    public final Island island;

    // Chance maps – populated by calculateChances()
    public final Map<Integer, Double>               distanceChances = new HashMap<>();
    public final Map<Integer, Double>               heightChances   = new HashMap<>();
    public final Map<BlockData, Double>             specialChances  = new HashMap<>();
    public final Map<BlockGenerationType, Double>   defaultChances  = new HashMap<>();

    // Schematic state
    public boolean deleteSchematic = false;
    public boolean waitForSchematicCompletion = false;
    public List<Block> schematicBlocks = new ArrayList<>();

    // History
    public List<Block> history = new LinkedList<>();
    public int lastPositionIndexPlayer = -1;
    public Location lastStandingPlayerLocation;

    // Delegates
    public final EffectManager effects;
    public final BlockPlacer placer;

    // ── Constructors ───────────────────────────────────────────────────────────

    public ParkourGenerator(@NotNull Session session, @Nullable LPSchematic schematic,
                            GeneratorOption... generatorOptions) {
        this.session = session;
        this.generatorOptions = Arrays.asList(generatorOptions);
        this.player = session.getPlayers().get(0);
        this.island = new Island(session, schematic);
        this.zone = Divider.toSelection(session);
        this.schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
        this.effects = new EffectManager(this);
        this.placer  = new BlockPlacer(this);
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
        if (s == null) LoParkour.getPlugin().getLogger()
                .warning("Island schematic '%s' not found!".formatted(name));
        return s;
    }

    public void overrideProfile() {}

    // ── Chances ────────────────────────────────────────────────────────────────

    protected void calculateChances() {
        defaultChances.clear();
        defaultChances.put(BlockGenerationType.DEFAULT,   Option.TYPE_NORMAL);
        defaultChances.put(BlockGenerationType.SCHEMATIC, Option.TYPE_SCHEMATICS);
        defaultChances.put(BlockGenerationType.SPECIAL,   Option.TYPE_SPECIAL);

        heightChances.clear();
        heightChances.put( 1, Option.NORMAL_HEIGHT_1);
        heightChances.put( 0, Option.NORMAL_HEIGHT_0);
        heightChances.put(-1, Option.NORMAL_HEIGHT_NEG1);
        heightChances.put(-2, Option.NORMAL_HEIGHT_NEG2);

        distanceChances.clear();
        distanceChances.put(1, Option.NORMAL_DISTANCE_1);
        distanceChances.put(2, Option.NORMAL_DISTANCE_2);
        distanceChances.put(3, Option.NORMAL_DISTANCE_3);
        distanceChances.put(4, Option.NORMAL_DISTANCE_4);

        specialChances.clear();
        specialChances.put(Material.PACKED_ICE.createBlockData(),                        Option.SPECIAL_ICE);
        specialChances.put(Material.SMOOTH_QUARTZ_SLAB.createBlockData("[type=bottom]"), Option.SPECIAL_SLAB);
        specialChances.put(Material.GLASS_PANE.createBlockData(),                        Option.SPECIAL_PANE);
        specialChances.put(Material.OAK_FENCE.createBlockData(),                         Option.SPECIAL_FENCE);
    }

    // ── Generation (delegates) ─────────────────────────────────────────────────

    public void generate()              { placer.generate(); }
    public void generate(int amount)    { placer.generate(amount); }

    public void generateFirst(Location spawn, Location block) {
        placer.generateFirst(spawn, block);
    }

    // ── Tick ───────────────────────────────────────────────────────────────────

    public void startTick() {
        task        = Scheduler.get(LoParkour.getPlugin()).runTimer(this::tick, 0, 1);
        cleanupTask = Scheduler.get(LoParkour.getPlugin()).runTimer(this::cleanupDistantBlocks, 0, Option.CLEANUP_INTERVAL);
    }

    public void tick() {
        if (stopped) { task.cancel(); if (cleanupTask != null) cleanupTask.cancel(); return; }

        getPlayers().forEach(p -> {
            updateVisualTime(p, p.selectedTime);
            p.updateScoreboard(this);
            p.player.setSaturation(20);
        });
        getSpectators().forEach(ParkourSpectator::update);

        if (player.getLocation().getWorld() != lastStandingPlayerLocation.getWorld()) return;
        if (player.getLocation().subtract(lastStandingPlayerLocation).getY() < -10) { fall(); return; }

        Block below = blockBelow();
        if (below == null) return;

        // Schematic end block
        if (schematicBlocks.contains(below) && below.getType() == Material.RED_WOOL && !deleteSchematic) {
            for (int i = 0; i < profile.get("schematicDifficulty").asDouble() * 15; i++) score();
            waitForSchematicCompletion = false;
            schematicCooldown = Config.GENERATION.getInt("advanced.schematic-cooldown");
            generate(profile.get("blockLead").asInt());
            deleteSchematic = true;
            return;
        }

        if (!history.contains(below)) return;

        int idx   = history.indexOf(below);
        int delta = idx - lastPositionIndexPlayer;
        if (delta <= 0) return;

        lastStandingPlayerLocation = player.getLocation();

        int lead = profile.get("blockLead").asInt();
        if (history.size() - idx <= lead) generate(lead - (history.size() - idx));
        lastPositionIndexPlayer = idx;

        // Remove trail blocks behind player
        for (int i = idx - BLOCK_TRAIL - 1; i >= idx - 4 * BLOCK_TRAIL; i--) {
            if (i > 0) history.get(i).setType(Material.AIR);
        }

        cleanupDistantBlocks();
        placer.deleteSchematic();

        int pts = Config.CONFIG.getBoolean("scoring.all-points") ? delta : 1;
        for (int i = 0; i < pts; i++) score();

        if (start == null) start = Instant.now();
    }

    private @Nullable Block blockBelow() {
        Location loc = player.getLocation().subtract(0, 1, 0);
        Block b = loc.getBlock();
        if (b.getType() == Material.AIR) {
            if (loc.subtract(0, 0.5, 0).getBlock().getType() == Material.AIR) return null;
            b = loc.getBlock();
        }
        return b;
    }

    private void updateVisualTime(ParkourPlayer p, int selectedTime) {
        int t = 18000 + selectedTime;
        if (t >= 24000) t -= 24000;
        p.player.setPlayerTime(t, false);
    }

    // ── Score ──────────────────────────────────────────────────────────────────

    protected void score() {
        score++;
        totalScore++;
        checkRewards();
        new ParkourScoreEvent(player).call();
    }

    private void checkRewards() {
        if (!Rewards.REWARDS_ENABLED || score == 0) return;

        if (Rewards.SCORE_REWARDS.containsKey(score))
            Rewards.SCORE_REWARDS.get(score).forEach(r -> r.execute(player, getMode()));

        int intervalScore = Config.CONFIG.getBoolean("scoring.rewards-use-total-score") ? totalScore : score;
        for (int interval : Rewards.INTERVAL_REWARDS.keySet()) {
            if (intervalScore % interval == 0)
                Rewards.INTERVAL_REWARDS.get(interval).forEach(r -> r.execute(player, getMode()));
        }

        String key = Integer.toString(score);
        if (Rewards.ONE_TIME_REWARDS.containsKey(score) && !player.collectedRewards.contains(key)) {
            Rewards.ONE_TIME_REWARDS.get(score).forEach(r -> r.execute(player, getMode()));
            player.collectedRewards.add(key);
        }
    }

    // ── Fall / Reset ───────────────────────────────────────────────────────────

    protected void fall() {
        new ParkourFallEvent(player).call();
        reset(true);
    }

    public void reset(boolean regenerate) {
        stopped = !regenerate;

        if (!regenerate && task == null)
            LoParkour.getPlugin().getLogger().warning("## Incomplete joining setup — report this!");

        lastPositionIndexPlayer = 0;
        if (!history.isEmpty()) {
            history.remove(0);
            history.forEach(b -> b.setType(Material.AIR, false));
            history.clear();
        }

        waitForSchematicCompletion = false;
        deleteSchematic = true;
        placer.deleteSchematic();

        Leaderboard lb = getMode().getLeaderboard();
        int record = lb != null ? lb.get(player.getUUID()).score() : 0;

        if (profile.get("showFallMessage").asBoolean()) sendFallMessage(record);
        if (lb != null && score > record)
            registerScore(getDetailedTime(), Double.toString(getDifficultyScore()).substring(0, 3), score);

        score = 0;
        start = null;
        heading = Option.HEADING.getDirection();

        if (regenerate) {
            player.teleport(playerSpawn);
            generateFirst(playerSpawn, blockSpawn);
            return;
        }

        island.destroy();
        if (getPlayers().isEmpty())
            getSpectators().forEach(s -> Modes.DEFAULT.create(s.player));
    }

    private void sendFallMessage(int record) {
        String key;
        int number = 0;
        if      (score == record) key = "settings.parkour_settings.items.fall_message.formats.tied";
        else if (score > record)  { key = "settings.parkour_settings.items.fall_message.formats.beat"; number = score - record; }
        else                      { key = "settings.parkour_settings.items.fall_message.formats.miss"; number = record - score; }

        for (ParkourPlayer p : getPlayers()) {
            p.sendTranslated("settings.parkour_settings.items.fall_message.divider");
            p.sendTranslated("settings.parkour_settings.items.fall_message.score",      Integer.toString(score));
            p.sendTranslated("settings.parkour_settings.items.fall_message.time",       getFormattedTime());
            p.sendTranslated("settings.parkour_settings.items.fall_message.high_score", Integer.toString(record));
            p.sendTranslated(key,                                                        Integer.toString(number));
            p.sendTranslated("settings.parkour_settings.items.fall_message.divider");
        }
    }

    protected void registerScore(String time, String difficulty, int score) {
        Leaderboard lb = getMode().getLeaderboard();
        if (lb == null) return;
        getPlayers().forEach(p -> lb.put(p.getUUID(), new Score(p.getName(), time, difficulty, score)));
    }

    // ── Cleanup ────────────────────────────────────────────────────────────────

    protected void cleanupDistantBlocks() {
        if (history.size() < Option.BLOCK_CLEANUP_DISTANCE * 2) return;
        Location loc = player.getLocation();
        int removed = 0;
        Iterator<Block> it = history.iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (b.getLocation().distance(loc) > Option.BLOCK_CLEANUP_DISTANCE) {
                b.setType(Material.AIR, false);
                it.remove();
                removed++;
            } else break;
        }
        if (removed > 0) lastPositionIndexPlayer = Math.max(0, lastPositionIndexPlayer - removed);
    }

    // ── Misc ───────────────────────────────────────────────────────────────────

    public void menu(ParkourPlayer player) { Menus.PARKOUR_SETTINGS.open(player); }

    public Block getLatest() { return history.get(history.size() - 1); }

    public double getDifficultyScore() {
        double s = 0;
        if (profile.get("useSpecialBlocks").asBoolean()) s += 0.5;
        double d = profile.get("schematicDifficulty").asDouble();
        if (d > 0) s += d <= 0.25 ? 0.2 : d <= 0.5 ? 0.3 : d <= 0.75 ? 0.4 : 0.5;
        return s;
    }

    public String getFormattedTime() { return getTime(Config.CONFIG.getString("options.time.score-format")); }
    public String getDetailedTime()  { return getTime("mm:ss:SSS"); }

    private String getTime(String format) {
        var ms = Instant.now().minusMillis(start != null ? start.toEpochMilli() : Instant.now().toEpochMilli());
        try {
            return DateTimeFormatter.ofPattern(format).withZone(ZoneOffset.UTC).format(ms);
        } catch (IllegalArgumentException ex) {
            LoParkour.getPlugin().getLogger().severe("Invalid time format: " + format);
            return "";
        }
    }

    public Mode getMode() { return Modes.DEFAULT; }

    @NotNull public List<ParkourPlayer>    getPlayers()    { return session.getPlayers(); }
    @NotNull public List<ParkourSpectator> getSpectators() { return session.getSpectators(); }

    public enum BlockGenerationType { DEFAULT, SCHEMATIC, SPECIAL }
}
