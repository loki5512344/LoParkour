# LoParkour — TODO

> Обновлено: 2026-03-07

## Легенда
- 🔴 Критично — баг/краш/не компилируется
- 🟡 Важно — фича заявлена но не работает
- 🟢 Планово — улучшение
- ✅ Готово

---

## 🔴 Критично

| # | Баг | Файл | Что сделать |
|---|-----|------|-------------|
| ✅ | ~~BOM в `ParkourGenerator.java` ломает компиляцию~~ | `ParkourGenerator.java` | ✅ Удалён через Python |
| ✅ | ~~`generator.score` → `generator.state.score`~~ | `PAPIHook.java`, `ParkourSettingsMenu.java` | ✅ Исправлено |
| ✅ | ~~`ParkourUser.joinCount` удалён~~ | `LoParkour.java` | ✅ Использован `UserRegistry.getJoinCount()` |
| ✅ | ~~`PlayerSettingsManager.OptionContainer` не public~~ | `PlayerSettingsManager.java` | ✅ Сделан public |
| ✅ | ~~`Option.PARTICLE_DATA.data()` не существует~~ | `EffectManager.java` | ✅ Убран `.data()` |
| ✅ | ~~`ParticleUtil.circle()` неправильные параметры~~ | `EffectManager.java` | ✅ Исправлено на (Location, Particle, int, int) |
| 1 | `PotionEffectType.JUMP` и `SLOW` удалены в Paper 1.20.5+ | `GravityShiftMode.java` | Заменить на `JUMP_BOOST` и `SLOWNESS` |
| 2 | `ElytraMode` создаёт обычный `ParkourGenerator` → `getMode()` возвращает DEFAULT | `ElytraMode.java` | Создать `ElytraGenerator extends ParkourGenerator` |
| 3 | `SpeedrunMode`: таймеры `runLater` продолжают тикать после `reset()` — гонка с генератором | `SpeedrunMode.java` | Хранить `ScheduledTask`, отменять в `reset()` |

---

## 🟡 Важно — фичи есть в коде, но не работают

| # | Фича | Файл | Что сделать |
|---|------|------|-------------|
| 4 | `HardcoreMode`: `collectedRewards` не очищается при падении | `HardcoreMode.java` | Добавить `player.collectedRewards.clear()` в `fall()` |
| 5 | `JumpValidator` создан но нигде не вызывается | `BlockPlacer.java` | Добавить retry-loop в `selectNext()` (до 10 попыток) |
| 6 | `JumpType` enum создан но не интегрирован в генератор | `BlockPlacer.java` | Добавить выбор JumpType по шансу из конфига |
| 7 | ~~`ConfigUpdater` закомментирован~~ | `Config.java` | ✅ Написан `ConfigUpdater.java`, подключён в `Config.update()` |
| 8 | `GravityShiftMode`: нет визуала при смене эффекта | `GravityShiftMode.java` | Добавить Title + Sound + партиклы `PORTAL` |

---

## 🟢 Планово — дуэли

### DuelMode
- [ ] `DuelMode implements Mode` — режим дуэли
- [ ] `DuelSession extends Session` — два игрока, отдельные острова
- [ ] `/lp duel <игрок>` — вызов на дуэль, запрос истекает через 30 сек
- [ ] Форматы: **Race** (кто первый до N очков), **Survival** (3 жизни), **Time Attack** (60 сек кто больше)
- [ ] Scoreboard показывает счёт обоих в реальном времени
- [ ] ActionBar: `Opponent: 34 pts` во время игры
- [ ] При падении противника — уведомление
- [ ] Победитель получает награды через систему `Rewards`
- [ ] Лидерборд дуэлей (W/L)
- [ ] Кнопка реванша после окончания

---

## 🟢 Планово

| # | Фича | Файл | Что сделать |
|---|------|------|-------------|
| 9 | Ghost система не интегрирована | `ghost/` | Подключить `GhostRecorder` к `GeneratorLifecycle` |
| ✅ | ~~Удалить мёртвые stub-файлы~~ | `generator/`, корень | ✅ Удалены: `Command.java`, `Events.java`, `BlockSelector.java`, `ScoreManager.java`, `GeneratorTick.java` |
| 11 | ElytraMode: кольца из партиклов + детекция пролёта | `ElytraMode.java` | BoundingBox check в `tick()` |
| 12 | ElytraMode: cooldown на фейерверки | `ElytraMode.java` | Счётчик + сообщение |
| 13 | RaceMode | новый файл | Кто первый наберёт N очков |
| 14 | CoopMode | новый файл | Общий счёт нескольких игроков |
| ✅ | ~~Реорганизация `generator/` по подпакетам~~ | `generator/` | ✅ Создана структура: `jump/`, `lifecycle/`, `effect/` |

---

## 🏗️ ✅ Реорганизация generator/ — ГОТОВО

Новая структура по логическим группам:

```
generator/
├── ParkourGenerator.java     ← главный класс (183 строки)
├── GeneratorState.java        ← данные генератора
├── GeneratorOption.java       ← enum опций
├── Profile.java               ← профиль настроек
├── Island.java                ← остров спавна
│
├── jump/                      ← всё про прыжки и размещение блоков
│   ├── BlockPlacer.java       (166 строк)
│   ├── JumpDirector.java
│   ├── JumpOffsetGenerator.java
│   ├── JumpValidator.java
│   └── JumpType.java
│
├── lifecycle/                 ← жизненный цикл: тик, падение, очистка
│   └── GeneratorLifecycle.java
│
└── effect/                    ← визуальные эффекты
    └── EffectManager.java
```

**Результат:**
- ✅ Все файлы перемещены через `smartRelocate` (импорты обновлены автоматически)
- ✅ Удалены мёртвые stub-файлы
- ✅ Проект компилируется без ошибок
- ✅ Структура стала логичнее и понятнее

---

## ✅ Уже готово (архитектура)

- Все критические баги из предыдущих сессий исправлены (UUID, SQL race, PreparedStatement, Leaderboard, Scoreboard, World NPE)
- `ParkourGenerator` разбит: `GeneratorState` + `GeneratorLifecycle` + `BlockPlacer` + `EffectManager`
- `Option` делегирует в `SqlOptions` / `ParticleOptions` / `GenerationOptions`
- `Option.SQL_URL/PORT/DB/USERNAME/PASSWORD/PREFIX` — forwarding-поля добавлены
- `GenerationOptions` — все новые пути защищены `isPath()` проверками
- `LoParkourCommand` разбит на `PlayerCommandHandler` + `AdminCommandHandler` + `SchematicCommandHandler`
- `Events` разбит на 3 listener'а
- `StorageSQL` разбит на `SQLConnectionManager` + `SQLQueryExecutor` + `SQLMigrationManager`
- ✅ **Проект компилируется без ошибок** (исправлены все баги после рефакторинга)

---

## 🎮 Чек-лист перед релизом

- [x] `./gradlew build` без ошибок
- [ ] Запуск сервера без ошибок в консоли
- [ ] Паркур: прыжки, счёт, рекорд работают
- [ ] Scoreboard обновляется
- [ ] Все меню открываются
- [ ] SQL: данные сохраняются и читаются
- [ ] Партиклы и звук в каждом режиме
- [ ] Все 5 режимов запускаются без ошибок
