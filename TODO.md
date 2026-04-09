# LoParkour — TODO

> Обновлено: 2026-04-09.

## Легенда

- 🔴 Критично — баг/краш/безопасность
- 🟡 Важно — заявлено, но не работает / серьёзные проблемы
- 🟢 Планово — улучшение / техдолг
- ✅ Готово

---

## Текущие задачи (2026-04-09)

### 🔴 P0 — Критичные баги

| # | Задача | Статус |
|---|--------|--------|
| 1 | Баг с локалью `'true'` в БД/конфиге | ✅ Исправлено (санитизация на всех уровнях) |
| 2 | Первый блок паркура генерится на платформе | ✅ Исправлено (6 блоков вперёд + 1 вверх) |
| 3 | ElytraMode полностью не работает | 🔴 **КРИТИЧНО** - отсутствуют классы |

### 🟡 P1 — Важные недоработки

| # | Задача | Статус |
|---|--------|--------|
| 4 | GravityShiftMode не реализован | 🟡 Только конфиг |
| 5 | HardcoreMode не реализован | 🟡 Только конфиг |

### 🟢 P2 — Проверка после исправлений

| # | Задача | Статус |
|---|--------|--------|
| 6 | DefaultMode после изменений Island.java | 🟢 Требует проверки |
| 7 | SpeedrunMode таймеры блоков | 🟢 Требует проверки |
| 8 | RaceMode прогресс-бар и финиш | 🟢 Требует проверки |
| 9 | CoopMode мультиплеер | 🟢 Требует проверки |

---

## Детальный план исправления режимов

### ElytraMode (4-6 часов)

**Проблема:** Отсутствуют классы, на которые ссылается `ElytraGenerator`:
- `ElytraConfig` - загрузка настроек из config.yml
- `ElytraRing` - модель кольца (центр, радиус, направление)
- `ElytraRingGenerator` - генерация колец по траектории
- `ElytraPhysics` - проверка пролёта, падения, буста
- `ElytraRenderer` - отрисовка частицами

**План:**
1. Создать `mode/elytra/ElytraConfig.java`
2. Создать `mode/elytra/ElytraRing.java`
3. Создать `mode/elytra/ElytraRingGenerator.java`
4. Создать `mode/elytra/ElytraPhysics.java`
5. Создать `mode/elytra/ElytraRenderer.java`
6. Исправить `generateFirst()` в `ElytraGenerator`
7. Добавить выдачу элитры и фейерверков

### GravityShiftMode (2-3 часа)

**План:**
1. Создать `GravityShiftMode.java`
2. Создать `GravityShiftGenerator extends ParkourGenerator`
3. Счётчик прыжков, каждые N прыжков - случайный эффект
4. Эффекты: jump-boost, speed, slowness, levitation
5. Зарегистрировать в `Modes.java`

### HardcoreMode (1-2 часа)

**План:**
1. Создать `HardcoreMode.java`
2. Создать `HardcoreGenerator extends ParkourGenerator`
3. Переопределить `fall()` - сбросить `collectedRewards`
4. Зарегистрировать в `Modes.java`

---

## Исправления от 2026-04-09

### ✅ Баг с локалью
- `ConfigAccessor.getString()` - форсирует String через `String.valueOf()`
- `Option.java:267-272` - санитизация при загрузке дефолтов из конфига
- `PlayerSettingsManager.java:37-44` - санитизация при загрузке настроек
- `SQLDataMapper.java:87-95` - санитизация при загрузке из SQL
- `StorageDisk.java:90-106` - санитизация при загрузке из JSON
- `SQLMigrationManager.java:51-53` - SQL миграция для исправления БД

### ✅ Первый блок паркура
- `Island.java:75-90` - первый блок на 6 блоков вперёд + 1 вверх от центра
- `GeneratorCleanup.java:110-127` - после падения тоже 6 вперёд + 1 вверх

### ✅ Spawn location в конфиге
- Добавлены `spawn-location` и `spawn-axes` в начало config.yml

---

## Главный план (стабилизация)

### P0 — блокер загрузки или геймплей

| # | Задача | Статус |
|---|--------|--------|
| 1 | LoLib 3.x в classpath | ✅ `build.gradle.kts` → `libs/lolib-3.0.0.jar` |
| 2 | `GeneratorCleanup` — хвост только **сзади** по индексу в `history` | ✅ |
| 3 | Потокобезопасность `history` | ✅ `synchronizedList`, снимок при полном `reset()` |

### P1 — важно

| # | Задача | Статус |
|---|--------|--------|
| 4 | Хотбар: клик по предмету | ✅ `isSimilar` в restriction listener |
| 5 | SQL `readPlayer` на корректном потоке | ✅ |

### P2 — проверить после P0–P1

| # | Задача | Статус |
|---|--------|--------|
| 6 | `LifecycleTickManager` / cleanup / time UI | ✅ |
| 7 | Scoring в воздухе | ✅ документировано (стояние на блоке) |

---

## Follow-up (сделано в коде)

- **Leaderboard**: sync-таймер; итерация/снимки без гонок с `synchronizedMap`.
- **AdminCommandHandler**: cooldown 2.5 с на `forcejoin`, `forceleave`, `reset`, `recoverinventory`; при блокировке — сообщение.
- **SQLConnectionManager**: HikariCP + `mysql-connector-j` в shadow.
- **SQL password**: если задана непустая **`LOPARKOUR_SQL_PASSWORD`**, она перекрывает `config.yml` → `sql.password`.
- **World**: валидация имени; папка через `getWorldContainer()`; безопасное удаление; кик только если мир не null.
- **LoParkourCommand**: `MultiMode` для join к другу; tab-complete; leaderboard без хака; «too many args».
- **PlayerCommandHandler**: `/parkour leaderboard` без второго аргумента открывает меню лидербордов.
- **RandomStyle** / **ElytraGenerator**: защита от пустых списков.
- **InventoryData**: новые сохранения в **YAML**; legacy binary читается один раз при миграции.

---

## Чек-лист перед релизом

- [ ] `./gradlew build`
- [ ] Старт сервера, `/parkour`, меню, один полный забег
- [ ] SQL: при проде выставить `LOPARKOUR_SQL_PASSWORD` на хосте
- [ ] Проверить миграцию старых `.dat` инвентарей при первом заходе игрока
- [ ] Протестировать все режимы: default, speedrun, race, coop, elytra, gravity-shift, hardcore
