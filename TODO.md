# LoParkour — TODO (обновлено 2026-06-01)

> Roadmap + **честный аудит** репозитория. Старые галочки «✅» в фазах 1–4 не соответствуют коду — см. раздел **Аудит**.

## Легенда

- 🔴 Критично — блокирует сборку / релиз / основной UX
- 🟡 Важно — заявленная функциональность или i18n
- 🟢 Планово — улучшение, рефакторинг
- ✅ Готово (проверено в коде)

---

## Аудит 2026-06-01 (снимок)

| Область | Факт |
|--------|------|
| **Сборка** | ✅ `./gradlew compileJava` проходит |
| **Адаптивность** | 🟡 Подключено: `AdaptiveServices`, `MetricsCollector` на генераторе, профиль после fall/reset |
| **Elytra** | 🟡 `ElytraMode` + `ElytraGenerator` есть; отдельного `mode/elytra/*` из старого TODO **нет** |
| **Размер файлов** | 🔴 Цель «все &lt; 200 строк» **не выполнена**: 18 файлов &gt; 150, 2 &gt; 200 |
| **i18n** | 🟡 Основные потоки через `Locales`; остаётся Phase 0 split + `MessageKeys` |
| **GUI** | 🟡 Основные меню на LoLib OK; **заглушки** `Menu`/`PagedMenu`, `DynamicMenu` вырезан |
| **TODO.md** | Ранее помечено ✅ то, что не сделано — статусы ниже исправлены |

**Статистика:** ~160 `.java` файлов, средний размер ~85 строк, максимум **252** (`ElytraGenerator.java`).

---

## Фаза 0: Полный рефакторинг (KISS · DRY · SOLID) — приоритет

> Цель: предсказуемая структура, тонкие классы, без «божественных» файлов.

### Правила (жёсткие)

| Правило | Значение |
|--------|----------|
| **Целевой размер** | ~**150** строк на файл |
| **Жёсткий потолок** | **190–200** строк (исключения только с комментом в PR) |
| **Файлов в папке** | ≤ **6** (при росте — новая подпапка) |
| **KISS** | Один публичный сценарий = один класс / use-case |
| **DRY** | Сообщения только через `Locales`; команды — `Messages` / `admin.*` |
| **SOLID** | `generator` не импортирует `adaptive`; связь только через `GeneratorEventListener` + DI в bootstrap |

### Целевая структура пакетов (✅ 2026-06-01)

```
dev.loki.loparkour/
├── LoParkour.java              # main plugin class
├── command/
│   ├── LoParkourCommand.java
│   ├── player/                 # PlayerCommandHandler
│   ├── admin/                  # AdminCommandHandler
│   └── schematic/              # SchematicCommandHandler
├── config/
│   ├── core/                   # Config, ConfigLoader, ConfigAccessor, ConfigUpdater
│   ├── locale/                 # Locales, LocaleLoader, LocaleCache
│   └── options/                # Option + OptionSQL, OptionGeneral, …
├── generator/
│   ├── core/                   # ParkourGenerator, Profile, GeneratorState, …
│   ├── jump/, lifecycle/, profile/, effect/
│   └── core/GeneratorEventListener
├── mode/
│   ├── base/                   # Mode, MultiMode, Modes, ModeMessages
│   ├── impl/                   # DefaultMode, SpeedrunMode, …
│   └── elytra/                 # ElytraGenerator + ring/physics/hud
├── menu/
│   ├── core/                   # LPMenu, MainMenu, Menus, ParkourOption
│   └── play/, settings/, community/, lobby/
├── player/
│   ├── core/                   # ParkourUser, ParkourPlayer
│   ├── service/                # UserRegistry, hotbar, scoreboard, …
│   ├── spectator/
│   └── data/
├── util/
│   ├── text/, item/, gui/, world/, particle/, misc/
├── storage/
│   ├── Storage.java            # фасад
│   ├── sql/                    # StorageSQL, SQL*, миграции
│   └── disk/                   # StorageDisk (JSON)
├── session/
│   ├── core/Session.java
│   └── manager/                # State, Player, User managers
├── listener/
│   ├── player/, gameplay/, schematic/
├── leaderboard/
│   ├── model/Score, core/, persistence/
├── ghost/
│   ├── model/, core/
├── hook/
│   ├── papi/, holo/, floodgate/, vault/
├── schematic/
│   ├── registry/Schematics.java
│   └── lpschem/
├── api/core/ + api/event/
├── reward/core/, style/core/, world/core/
├── adaptive/bootstrap/AdaptiveServices
├── bootstrap/PluginBootstrap
└── …
```

Скрипт переноса: `scripts/refactor-packages.ps1` (+ `fix-imports-after-refactor.ps1`).

### Очередь разбиения (файлы &gt; 150 строк)

| Приоритет | Файл | Строк | Действие |
|-----------|------|-------|----------|
| ✅ 1 | `ElytraGenerator.java` | 101 | → `elytra/ElytraRingLoop`, `ElytraScoring`, `ElytraHud`, `ElytraPlayerKit`, `ElytraFireworkListener` |
| ✅ 2 | `ParkourPlayer.java` | 166 | → `ParkourHotbar` |
| 🟡 3 | `PAPIHook.java` | 194 | → placeholders по группам |
| 🟡 4 | `LoParkour.java` | 192 | → `bootstrap/PluginBootstrap.java` |
| 🟡 5 | `MetricsCollector.java` | 187 | → session hook + jump hook + flush |
| 🟡 6 | `StorageSQL.java` | 179 | → queries / mapper / connection (уже частично) |
| 🟡 7 | `BlockPlacer.java` | 176 | | |
| 🟡 8 | `JumpValidator.java` | 172 | | |
| 🟡 9 | `LoParkourCommand.java` | 170 | → делегаты только маршрутизация |
| 🟡 10 | `CoopMode.java` | 165 | → `CoopGenerator` отдельный файл |
| 🟢 | остальные 151–159 | 8 файлов | после топ-10 |

### Чек-лист фазы 0

- [ ] Нет файлов &gt; 200 строк (кроме явно согласованных)
- [x] Пакеты разнесены (config/mode/command/generator/menu/player/util)
- [ ] Нет циклических зависимостей `generator` ↔ `adaptive`
- [ ] Единый `MessageService` / `Locales` для player + console + action bar + title
- [ ] Удалить или подключить мёртвый код: `Menu`, `PagedMenu`, `DynamicMenu`, `MenuStub`
- [ ] `Option.java` — только фасад, без роста (делегаты в `options/*`)
- [x] `./gradlew build` зелёный
- [ ] README/TODO не врут про «всё готово»

---

## Баги и долг (найдено при аудите)

### 🔴 Сборка

| # | Проблема | Статус |
|---|----------|--------|
| B1 | `SQLConnectionManager` visibility | ✅ |
| B2 | `AdaptiveConfig` + `Config.CONFIG` | ✅ |

### 🟡 Локализация (i18n) — большинство закрыто

| # | Проблема | Статус |
|---|----------|--------|
| I1 | `LPMenu` back/next | ✅ `other.back`, `other.next` |
| I2 | `other.no_menu_items` | ✅ |
| I3 | joining disabled | ✅ `ModeMessages` + `other.joining_disabled` |
| I4 | Elytra messages | ✅ `modes.elytra.*` |
| I5–I11 | Race, coop, admin, schematic, commands, bungee, GUI stubs | ✅ (см. `locales/*.yml`) |
| I12 | `ghost.saved` в en | ✅ |
| I13 | `%[a-z]` vs `%s` | 🟢 документировать / унифицировать позже |
| I14 | reload NPE с консоли | ✅ `getString(sender, …)` |
| I15 | недостающие ключи | ✅ основной набор добавлен |

### 🟡 Логика / архитектура

| # | Проблема | Где |
|---|----------|-----|
| A1 | `MetricsCollector` на генераторе | ✅ `SessionStateManager` + `AdaptiveServices.attachToGenerator` |
| A2 | Adaptive → профиль генерации | ✅ `GeneratorProfileManager` + `AdaptiveDifficulty` |
| A3 | `adaptive.*` в config.yml | ✅ |
| A4 | SQL таблица stats | ✅ `StorageSQL` + `SQLMigrationManager.createAdaptiveStatsTable` |
| A5 | `DynamicMenu` мёртвый | 🔴 |
| A6 | STONE debug-spam | ✅ только при отсутствии `.material` |
| A7 | Несогласованные пути locale: `play.single.*` vs `modes.*` для режимов | `*Mode.getItem()` |
| A8 | `sendHelp` упоминает `/LoParkour`, игроки используют `/parkour` | `PlayerCommandHandler` |

### 🟢 Мелочи

| # | Проблема |
|---|----------|
| M1 | `SpectatorMode`: MiniMessage `<red>` vs legacy `§c` в других режимах |
| M2 | `HardcoreMode` / др.: смешение `ColorUtil` и сырого `sendMessage` |
| M3 | Unit/integration тесты в TODO помечены ✅ — **фактически** только 2 test-класса |

---

## Фаза 1: Рефакторинг структуры (статус реальный)

### Архитектура

| # | Задача | Статус |
|---|--------|--------|
| 1 | Разбить `ParkourGenerator` | 🟡 Частично (composition), файл 159 строк — ок |
| 2 | `GeneratorProfileManager` | 🟡 Есть, без отдельного `ProfileCalculator` |
| 3 | statistics из generator | 🟡 `GeneratorStatistics` внутри generator, не пакет `statistics/` |
| 4 | Структура пакетов `generator/core`, `jump`, `lifecycle` | 🟡 Частично |
| 5 | `GeneratorEventListener` | ✅ Интерфейс + вызовы в `PlayerInteractionHandler` |

### Большие файлы (цель &lt; 200) — **не выполнено**

| Файл | Строк | Статус |
|------|-------|--------|
| ElytraGenerator.java | 101 | ✅ |
| ParkourPlayer.java | 166 | ✅ |
| PAPIHook, LoParkour, MetricsCollector, … | 151–194 | 🟡 см. Фаза 0 |

---

## Фаза 2: Адаптивная система (статус реальный)

| # | Задача | Статус |
|---|--------|--------|
| 1 | `adaptive/model/` | 🟡 Классы есть |
| 2 | `adaptive/storage/` | ✅ compile + SQL/file repo |
| 3 | SQL таблицы | ✅ CREATE в `StorageSQL` / `SQLMigrationManager` |
| 4 | File JSON storage | ✅ |
| 5 | `MetricsCollector` | ✅ |
| 6 | Calculator / Adjuster в профиле | ✅ через `AdaptiveDifficulty` |
| 7 | `AdaptiveMode`, `StatsService` | 🔴 отдельный режим не нужен пока |
| 8 | `config.yml` → `adaptive.*` | ✅ |

---

## Фаза 3: ElytraMode (статус реальный)

| # | Задача | Статус |
|---|--------|--------|
| 1 | `ElytraMode` | 🟡 Есть |
| 2 | `ElytraGenerator` + helpers | ✅ Разбит на `mode/elytra/*` |
| 3 | `ElytraConfig`, `RingRenderer`, … | ✅ В пакете `mode/elytra/` |
| 4 | `modes/elytra.yml` | 🔴 Нет |
| 5 | i18n для elytra | ✅ |

---

## Фаза 4: Режимы (статус реальный)

| Режим | Меню | i18n join disabled | Примечание |
|-------|------|-------------------|------------|
| default, speedrun, hardcore, gravity-shift | ✅ SingleMenu | 🔴 хардкод EN | |
| elytra, race | ✅ | 🔴 | race — ещё UI хардкод |
| coop | ❌ (null item — ок) | 🔴 | title хардкод |
| spectator | ✅ | 🟡 частично Locales | |
| roguelike | — | — | **Нет в коде** (в старом TODO было ✅ — ошибка) |

---

## Фаза 5: i18n — единая система сообщений

| # | Задача | Статус |
|---|--------|--------|
| 1 | `other.back` / `other.next` | ✅ |
| 2 | Ключи из таблицы I* | ✅ |
| 3 | `MessageKeys` enum или constants class (DRY путей) | 🟢 |
| 4 | `Locales.getString(CommandSender)` — использовать везде для консоли | 🟡 Частично |
| 5 | Убрать debug-spam STONE | ✅ |
| 6 | Синхронизировать `en.yml` ↔ `ru.yml` | 🟡 периодический diff |
| 7 | Проверка CI: скрипт diff ключей locale vs `Locales.getString`/`sendTranslated` | 🟢 |

---

## Технический долг (актуальный)

| # | Задача | Приоритет |
|---|--------|-----------|
| 1 | Compile adaptive | ✅ |
| 2 | Bootstrap MetricsCollector | ✅ |
| 3 | SQL adaptive table | ✅ |
| 4 | Удалить или реализовать `Menu`/`PagedMenu`/`DynamicMenu` | 🟡 |
| 5 | Реальные тесты adaptive + modes | 🟢 |

---

## Чек-лист перед релизом (честный)

- [x] `./gradlew build` без ошибок
- [x] Нет хардкода RU в `LPMenu` / основных режимах и админке
- [ ] Полный diff ключей locale vs Java (CI)
- [ ] Адаптивность: ручной тест на сервере (`adaptive.enabled: true`)
- [ ] ElytraMode протестирован вручную
- [ ] Нет файлов &gt; 200 строк (или исключения задокументированы)
- [ ] README / CHANGELOG соответствуют коду

---

## SQL миграция (без изменений)

```sql
CREATE TABLE IF NOT EXISTS loparkour_player_stats (
    player_uuid VARCHAR(36) PRIMARY KEY,
    skill_rating DOUBLE DEFAULT 1.0,
    sessions_count INT DEFAULT 0,
    total_jumps INT DEFAULT 0,
    total_falls INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_skill_rating (skill_rating),
    INDEX idx_last_updated (last_updated)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## Оценка времени (пересмотр)

| Фаза | Дней | Комментарий |
|------|------|-------------|
| **0: KISS/DRY/SOLID + лимит строк** | 5–8 | Блокирует нормальную поддержку |
| **5: i18n** | 2–3 | Можно параллельно с 0 |
| **2: Adaptive** | 3–4 | После зелёной сборки + bootstrap |
| **3: Elytra split** | 1–2 | После 0 |
| **4: Режимы / тесты** | 2–3 | |
| **Итого** | **13–20** | Старые «8–12 дней» занижены |

---

## Метрики успеха (пересмотр)

### Технические
- [ ] ≥ 90% файлов ≤ 150 строк
- [ ] 0 файлов &gt; 200 строк
- [ ] SOLID: `adaptive` → listener/port, не прямой import generator internals
- [ ] `./gradlew test` — осмысленное покрытие, не «галочка в TODO»

### Функциональные
- [ ] 2+ языка без хардкода в Java (кроме логов debug)
- [ ] Adaptive влияет на профиль после N сессий
- [ ] ElytraMode без монолита 250+ строк

### Производительность
- [ ] TPS стабилен 50+ игроков (профилирование)
- [ ] SQL batch / flush adaptive &lt; 50ms
