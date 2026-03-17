# LoParkour — Гайд по патчам

Все файлы в этом архиве заменяют оригинальные файлы проекта **1 к 1 по пути**.
Просто скопируй их поверх своего src/ и пересобери проект.

---

## Что уже исправлено (этот архив)

### 🔴 Патч 1 — SQL Injection (КРИТИЧНО)
**Файлы:** `storage/StorageSQL.java`, `storage/SQLQueryExecutor.java`, `storage/SQLMigrationManager.java`

Оригинальный код вставлял имена игроков, стили, локали напрямую в SQL строки через `.formatted()`.
Игрок с ником `'; DROP TABLE loparkour-leaderboard-default; --` мог уничтожить таблицу лидерборда.

**Что сделано:** Все запросы с пользовательскими данными переписаны на `?` плейсхолдеры.
JDBC-драйвер сам экранирует значения — инъекция физически невозможна.

---

### 🔴 Патч 2 — ElytraMode полная переработка
**Файлы:** `mode/ElytraMode.java`, `mode/ElytraGenerator.java`, `resources/config.yml`, `resources/locales/en.yml`

Оригинальный ElytraMode был просто обычным паркуром с элитрой в инвентаре.
Кольца лежали горизонтально (как блины), хитбокс был кубом 5×5×5,
cooldown фейерверка не работал (показывал сообщение но не блокировал).

**Что сделано:**
- Генерируются кольца в воздухе по плавной кривой (настраивается в config.yml)
- Кольца стоят вертикально, перпендикулярно направлению полёта
- Детект пролёта через плоскость кольца (dot product, а не куб)
- Firework перехватывается через `PlayerInteractEvent` — реально блокируется до использования
- Boost выдаётся вручную через `setVelocity()` + визуальный фейерверк без урона
- Action bar: счёт + дистанция до следующего кольца + cooldown

---

### 🟠 Патч 3 — SpeedrunMode не работал вообще
**Файл:** `mode/SpeedrunMode.java`

`onBlockTouch()` был объявлен но **нигде не вызывался** — блоки никогда не исчезали.
Speedrun был идентичен обычному режиму.

**Что сделано:** Подключился через `score()` override — вызывается в момент когда
игрок встаёт на новый блок. Исправлено деление на 50 которое давало 0 тиков при
малых значениях. Warning task теперь отменяется при reset().

---

### 🟠 Патч 4 — CoopMode показывался в одиночном меню
**Файл:** `mode/CoopMode.java`

`CoopMode implements Mode` вместо `MultiMode` — поэтому висел в меню рядом с Default/Speedrun.
`SingleMenu` фильтрует `instanceof MultiMode`, но кооп им не был.

**Что сделано:** `CoopMode implements MultiMode`. `getItem()` возвращает `null`.
`create()` открывает лобби вместо создания сессии соло.
Исправлен двойной счётчик (`sharedScore` дублировал `state.score`).
`fall()` больше не чистит стейт который уже сброшен внутри `super.fall()`.

---

### 🟠 Патч 5 — RaceMode off-by-one + ActionBar конфликт
**Файл:** `mode/RaceMode.java`

Проверка финиша была в `tick()` — срабатывала на тик позже нужного.
ActionBar обновлялся каждый тик конфликтуя с parent.
`reset(false)` вызывался с задержкой без null-check — NPE если игрок вышел.

**Что сделано:** Финиш проверяется в `score()` override — точно в момент достижения цели.
Прогресс-бар обновляется только при новом очке. Добавлен guard `!state.stopped`.

---

## Что ещё нужно исправить (следующие шаги)

### 🔴 Приоритет 1 — Ghost файлы по имени игрока (path traversal + коллизии)
**Файл:** `ghost/GhostManager.java`

```java
// ПРОБЛЕМА:
File file = new File(modeFolder, data.getPlayerName() + ".ghost");
```

Два игрока с одинаковым именем затирают друг друга.
Игрок с именем `../../config` может писать за пределы папки плагина.

**Как чинить:** Называть файлы по UUID игрока, не по имени.
Хранить имя внутри файла (оно уже там есть в `GhostData`).

---

### 🟡 Приоритет 2 — Ghost не пишет всех игроков в мультиплеере
**Файл:** `generator/lifecycle/GeneratorLifecycle.java`

`GhostRecorder` записывает только `generator.player` (первый игрок сессии).
В CoopMode и RaceMode остальные игроки не записываются.

**Как чинить:** Recorder на каждого игрока отдельно, или писать только в single-player режимах.

---

### 🟡 Приоритет 3 — `disable()` глотает все исключения
**Файл:** `LoParkour.java`

```java
} catch (Throwable ignored) { }
```

Если при выключении сервера не сохранятся данные — никто не узнает.

**Как чинить:** Минимум логировать: `catch (Throwable t) { getLogger().severe(...) }`.

---

### 🟡 Приоритет 4 — `cleanupDistantBlocks()` вызывается дважды за тик
**Файл:** `generator/lifecycle/GeneratorLifecycle.java`

Вызывается явно в `tick()` И через отдельный `cleanupTask` таймер одновременно.

**Как чинить:** Убрать явный вызов из `tick()`, оставить только таймер.

---

### 🟡 Приоритет 5 — `new Random()` в горячих методах
**Файл:** `generator/jump/BlockPlacer.java`

```java
pool.get(new java.util.Random().nextInt(pool.size()))  // внутри tryGenerateSchematic()
new java.util.Random().nextInt(rotations.length)        // внутри rotatedPaste()
```

Каждый вызов создаёт новый объект Random. Дорого и неправильно.

**Как чинить:** Один `private static final ThreadLocalRandom RNG = ThreadLocalRandom.current()`
на уровне класса.

---

### 🟡 Приоритет 6 — `System.err.println` в JumpOffsetGenerator
**Файл:** `generator/jump/JumpOffsetGenerator.java`

```java
System.err.println("Warning: Unexpected jump height ...");
```

В Bukkit плагинах нельзя писать в System.err напрямую.

**Как чинить:** `LoParkour.getPlugin().getLogger().warning(...)`.

---

### 🟢 Приоритет 7 — Пустой javadoc в LoParkour.java
**Файл:** `LoParkour.java`

Висит осиротевший javadoc от удалённого метода `getLogging()`. Не баг, просто мусор.

---

## Порядок следующих патчей (рекомендация)

1. Ghost path traversal (безопасность)
2. `disable()` silent catch (надёжность)
3. `cleanupDistantBlocks` двойной вызов (производительность)
4. `new Random()` в горячих путях (производительность)
5. Ghost мультиплеер (функциональность)

---

## Патчи добавленные в эту версию архива

### 🔴 Патч 6 — Ghost path traversal + коллизии по имени (БЕЗОПАСНОСТЬ)
**Файлы:** `ghost/GhostData.java`, `ghost/GhostManager.java`, `ghost/GhostRecorder.java`, `generator/lifecycle/GeneratorLifecycle.java`

Файлы назывались `playerName.ghost`. Два игрока с одинаковым ником затирали друг друга.
Игрок с именем `../../config` мог писать файлы за пределы папки плагина (path traversal).

**Что сделано:**
- Файлы теперь называются `uuid.ghost` — уникально и filesystem-safe
- `GhostData` хранит UUID (добавлен в конструктор и в бинарный формат)
- `GhostRecorder.stopRecording()` принимает UUID + имя
- `GhostManager.sanitizeMode()` чистит имя режима от слэшей
- Старые файлы v1 (без UUID) автоматически удаляются при загрузке с логом
- Формат файла версионирован (int VERSION=2 в начале)

---

### 🟡 Патч 7 — `cleanupDistantBlocks()` вызывался дважды за тик
**Файл:** `generator/lifecycle/GeneratorLifecycle.java`

Был явный вызов в `tick()` И таймер `cleanupTask` — двойная работа каждый тик.

**Что сделано:** Убран явный вызов из `tick()`. Только таймер.

---

### 🟡 Патч 8 — `new Random()` в горячих методах BlockPlacer
**Файл:** `generator/jump/BlockPlacer.java`

`new java.util.Random()` создавался внутри `tryGenerateSchematic()` и `rotatedPaste()` при каждом вызове.

**Что сделано:** Заменено на `ThreadLocalRandom.current().nextInt()`.

---

### 🟡 Патч 9 — `System.err.println` в JumpOffsetGenerator
**Файл:** `generator/jump/JumpOffsetGenerator.java`

**Что сделано:** Заменено на `LoParkour.getPlugin().getLogger().warning(...)`.

---

### 🟡 Патч 10 — `disable()` глотал все исключения молча
**Файл:** `LoParkour.java`

`catch (Throwable ignored) {}` — если данные не сохранялись при выключении сервера, никто не узнавал.

**Что сделано:** `catch (Throwable t)` с логированием и `t.printStackTrace()`.

---

## Оставшиеся задачи

### 🟡 Ghost мультиплеер
`GhostRecorder` пишет только `generator.player` (первый игрок сессии).
В CoopMode/RaceMode остальные игроки не записываются.
**Решение:** Либо отдельный рекордер на каждого, либо запись только в single-player режимах.

### 🟢 Осиротевший javadoc в LoParkour.java
Комментарий от удалённого метода `getLogging()` — просто мусор, не баг.
