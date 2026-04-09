# GravityShift и Hardcore Modes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Реализовать GravityShiftMode (случайные эффекты зелий) и HardcoreMode (сброс наград при падении)

**Architecture:** Оба режима следуют паттерну Mode + внутренний Generator класс. GravityShiftGenerator переопределяет score() для применения эффектов, HardcoreGenerator переопределяет fall() для сброса наград.

**Tech Stack:** Bukkit/Spigot API, Java 21, существующая архитектура Mode/ParkourGenerator

---

## Task 1: Implement GravityShiftMode

**Files:**
- Create: `src/main/java/dev/loki/loparkour/mode/GravityShiftMode.java`

- [ ] **Step 1: Create GravityShiftMode with inner Generator**

```java
package dev.loki.loparkour.mode;

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
                    applyRandomEffect(pp.getPlayer());
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
                Player player = pp.getPlayer();
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
            return Modes.GRAVITY_SHIFT;
        }

        private record EffectConfig(int amplifier, int durationTicks) {}
    }
}
```

- [ ] **Step 2: Commit GravityShiftMode**

```bash
git add src/main/java/dev/loki/loparkour/mode/GravityShiftMode.java
git commit -m "feat: implement GravityShiftMode with random potion effects"
```

---

## Task 2: Implement HardcoreMode

**Files:**
- Create: `src/main/java/dev/loki/loparkour/mode/HardcoreMode.java`

- [ ] **Step 1: Create HardcoreMode with inner Generator**

```java
package dev.loki.loparkour.mode;

import dev.loki.loparkour.config.Config;
import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.leaderboard.Leaderboard;
import dev.loki.loparkour.player.ParkourPlayer;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.util.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hardcore Mode — lose all collected rewards on fall.
 */
public class HardcoreMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "hardcore";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return Locales.getItem(locale, "modes.hardcore");
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
        if (pp != null && pp.session.generator instanceof HardcoreGenerator) {
            return;
        }

        player.closeInventory();
        Session.create(session -> new HardcoreGenerator(session), null, null, player);
    }

    // ── HardcoreGenerator ─────────────────────────────────────────────────────

    private static class HardcoreGenerator extends ParkourGenerator {

        private final boolean resetRewards;

        public HardcoreGenerator(@NotNull Session session) {
            super(session);
            this.resetRewards = Config.CONFIG.isPath("modes.hardcore.reset-rewards")
                ? Config.CONFIG.getBoolean("modes.hardcore.reset-rewards") : true;
        }

        @Override
        public void fall() {
            if (resetRewards) {
                for (ParkourPlayer pp : getPlayers()) {
                    pp.collectedRewards.clear();
                }
            }

            super.fall();
        }

        @Override
        public Mode getMode() {
            return Modes.HARDCORE;
        }
    }
}
```

- [ ] **Step 2: Commit HardcoreMode**

```bash
git add src/main/java/dev/loki/loparkour/mode/HardcoreMode.java
git commit -m "feat: implement HardcoreMode with reward reset on fall"
```

---

## Task 3: Register modes in Modes.java

**Files:**
- Modify: `src/main/java/dev/loki/loparkour/mode/Modes.java`

- [ ] **Step 1: Read Modes.java to find registration pattern**

```bash
cat src/main/java/dev/loki/loparkour/mode/Modes.java
```

- [ ] **Step 2: Add static fields for new modes**

Add after existing mode declarations (e.g., after `SPEEDRUN`, `ELYTRA`):

```java
public static GravityShiftMode GRAVITY_SHIFT;
public static HardcoreMode HARDCORE;
```

- [ ] **Step 3: Register modes in init() method**

Add in `init()` method after existing registrations:

```java
GRAVITY_SHIFT = register(new GravityShiftMode());
HARDCORE = register(new HardcoreMode());
```

- [ ] **Step 4: Commit registration**

```bash
git add src/main/java/dev/loki/loparkour/mode/Modes.java
git commit -m "feat: register GravityShift and Hardcore modes"
```

---

## Task 4: Add locale entries

**Files:**
- Modify: `src/main/resources/lang/en.yml`
- Modify: `src/main/resources/lang/ru.yml`

- [ ] **Step 1: Add English locale entries**

Add to `en.yml` under `modes:` section:

```yaml
  gravity-shift:
    item: POTION
    name: 'Gravity Shift'
    description: 'Random potion effects every few jumps'
  hardcore:
    item: SKELETON_SKULL
    name: 'Hardcore'
    description: 'Lose all rewards on fall'
```

- [ ] **Step 2: Add Russian locale entries**

Add to `ru.yml` under `modes:` section:

```yaml
  gravity-shift:
    item: POTION
    name: 'Гравитационный сдвиг'
    description: 'Случайные эффекты зелий каждые несколько прыжков'
  hardcore:
    item: SKELETON_SKULL
    name: 'Хардкор'
    description: 'Потеря всех наград при падении'
```

- [ ] **Step 3: Commit locale entries**

```bash
git add src/main/resources/lang/en.yml src/main/resources/lang/ru.yml
git commit -m "feat: add locale entries for GravityShift and Hardcore modes"
```

---

## Task 5: Test both modes

**Files:**
- None (manual testing)

- [ ] **Step 1: Build plugin**

```bash
./gradlew clean build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Copy to server**

```bash
cp build/libs/LoParkour-*.jar /path/to/server/plugins/
```

- [ ] **Step 3: Start server and test GravityShift**

1. Start server
2. Join parkour with `/lp`
3. Switch to gravity-shift mode
4. Make 10+ jumps
5. Verify random potion effects appear

Expected: Effects like Jump Boost, Speed, Slowness, Levitation apply randomly every 10 jumps

- [ ] **Step 4: Test Hardcore mode**

1. Switch to hardcore mode
2. Collect some rewards (if available)
3. Fall off parkour
4. Check if rewards were cleared

Expected: `player.collectedRewards` should be empty after fall

- [ ] **Step 5: Verify config loading**

Check server logs for any config errors related to:
- `modes.gravity-shift.interval`
- `modes.gravity-shift.effects.*`
- `modes.hardcore.reset-rewards`

Expected: No errors, default values used if config missing

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-09-gravity-shift-hardcore-modes.md`.

**Two execution options:**

**1. Subagent-Driven (recommended)** - Fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
