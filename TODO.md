# LoParkour — TODO & Roadmap

> **Принципы:** KISS · DRY · SOLID  
> **Версия:** 1.3.0 → 2.0.0  
> **Обновлено:** 2026-03-05

---

## 📊 Статус миграции vilib → LoLib 2.0

✅ **МИГРАЦИЯ ЗАВЕРШЕНА** (2026-03-01)
- Файлов изменено: 120+
- Строк кода: 3000+
- Ошибок исправлено: 250+
- Размер jar: 655 КБ
- Все меню работают
- Схематики работают

---

## Легенда
- 🔴 **Критично** — баги/краши, нужно прямо сейчас
- 🟡 **Важно** — неработающие фичи
- 🟢 **Планово** — улучшения и новые фичи
- ✅ **Готово**

---

## 🔴 Критично

| # | Задача | Файл | Статус |
|---|--------|------|--------|
| 1 | GUIManager.init() не вызывался — клики не отменялись | `LoParkour.java` | ✅ |
| 2 | SQL race condition: CREATE TABLE до connect() | `StorageSQL.java` | ✅ |
| 3 | PreparedStatement leak в sendQuery() | `StorageSQL.java` | ✅ |
| 4 | Leaderboard читал данные до готовности SQL | `Leaderboard.java` | ✅ |
| 5 | Scoreboard не назначался игроку / обновление отключено | `ParkourUser.java` | ✅ |
| 6 | Только 16 уникальных entries в scoreboard (§0..§f) | `ParkourUser.java` | ✅ |
| 7 | NPE в World.setup() если createWorld() упал | `World.java` | ✅ |
| 8 | World.world не обновлялся при крэш-рестарте | `World.java` | ✅ |
| 9 | UUID сравнивался через == вместо .equals() | `ParkourUser.java` | ✅ |
| 10 | Async world creation вызывалось не на main thread | `LoParkour.java` | ✅ |
| 11 | StorageSQL.close() не проверяет connection == null | `StorageSQL.java` | ✅ |

---

## 🟡 Важно — неработающие фичи

### Партиклы
**Файл:** `generator/ParkourGenerator.java` → `generator/EffectManager.java`  
- [x] Реализован базовый DOT/CIRCLE/BOX через `EffectManager`
- [ ] Протестировать все три режима в игре
- [ ] Убедиться что `ParticleUtil.circle()` принимает `List<Player>`

### ConfigUpdater
**Файл:** `config/Config.java`  
- [ ] Раскомментировать `ConfigUpdater.update(...)` (закомментировано с TODO)
- [ ] Убедиться что `ignoredSections` правильно пробрасываются

### SpeedrunMode — блоки не синхронизированы с генератором
**Файл:** `mode/SpeedrunMode.java`  
- [ ] Переопределить `tick()` в `SpeedrunGenerator`
- [ ] Блоки должны исчезать через `history`, а не независимо

### HardcoreMode — сброс наград при падении
**Файл:** `mode/HardcoreMode.java`  
- [ ] Переопределить `reset()` в `HardcoreGenerator`
- [ ] Очищать `player.collectedRewards` при `regenerate=true`

---

## 🟢 Планово — новые фичи

### ElytraMode — кольца
**Файл:** `mode/ElytraMode.java`  
- [ ] `ElytraGenerator extends ParkourGenerator` — генерировать кольца из партиклов
- [ ] Детекция пролёта через кольцо (bounding box check в tick)
- [ ] Cooldown на фейерверки (буст)

### GravityShiftMode — визуальные эффекты
**Файл:** `mode/GravityShiftMode.java`  
- [ ] Title при смене эффекта
- [ ] Партиклы PORTAL при смене
- [ ] Звук при смене

### Валидация прыжков
**Файл:** `generator/ParkourGenerator.java`  
- [ ] Включить `JUMP_VALIDATION_ENABLED` из конфига в `selectNext()`
- [ ] Retry-loop: до 10 попыток если прыжок невозможен
- [ ] Использовать уже существующий `JumpValidator`

### Специальные прыжки
**Файл:** `generator/JumpType.java` (создать)  
- [ ] neo-jump (4 блока по диагонали)
- [ ] head-hitter (блок над головой)
- [ ] Интегрировать в `selectBlocks()` через шанс из конфига

### Ghost система
**Файл:** `ghost/`  
- [ ] Протестировать `GhostRecorder` и `GhostPlayer`
- [ ] Включить в конфиге `ghost-mode.enabled: true`
- [ ] Команда `/lp ghost <on|off>`

### Мультиплеер режимы
- [ ] RaceMode (гонка — кто первый наберёт N очков)
- [ ] CoopMode (кооператив — общий счёт)

### Локализация режимов
**Файлы:** `locales/en.yml`, `locales/ru.yml`  
- [ ] gravity-shift: название эффекта, `effect-applied`
- [ ] elytra: `ring-passed`, `boost-ready`, `boost-cooldown`
- [ ] hardcore: `rewards-lost`

---

## ✅ Архитектурный рефакторинг — завершён

| Что сделано | Результат |
|-------------|-----------|
| `Command.java` → удалён (дубликат) | `LoParkourCommand.java` — единственный файл |
| `LoParkourCommand.java` (528 строк) | `command/PlayerCommandHandler` + `AdminCommandHandler` + `SchematicCommandHandler` + тонкий роутер |
| `Events.java` (268 строк) | `listener/PlayerConnectionListener` + `ParkourRestrictionListener` + `SchematicWandListener` |
| `ParkourGenerator.java` (666 строк) | Делегирует в `EffectManager` + `GeneratorTick` |
| `Option.java` (247 строк) | `SqlOptions` + `ParticleOptions` + `GenerationOptions` + тонкий `Option` |
| GUI-паттерн (13 меню) | Исправлены на правильный `create()` API |
| Scoreboard | Полная реализация через Team prefix API |
| SQL инициализация | `pendingTableCreations` queue + `onConnectCallbacks` |

---

## 🎮 Чек-лист перед релизом

- [ ] `./gradlew build` без ошибок и предупреждений
- [ ] Запуск сервера без ошибок в консоли
- [ ] Паркур создаётся и работает (прыжки, счёт, рекорд)
- [ ] Скорборд обновляется во время игры
- [ ] Все меню открываются корректно
- [ ] SQL: таблицы создаются, данные сохраняются/читаются
- [ ] Схематики: save → reload → paste работает
- [ ] Лидерборд: запись и чтение рекордов
- [ ] Частицы и звук работают в каждом режиме
- [ ] `/lp forcejoin`, `forceleave`, `reset`, `recoverinventory`


---

## 🔧 План рефакторинга больших файлов

### Проблемные файлы (>200 строк):
- `generator/ParkourGenerator.java` (306) → разбить на 3 файла
- `storage/StorageSQL.java` (248) → разбить на 2 файла
- `session/Session.java` (236) → разбить на 2 файла
- `player/ParkourUser.java` (220) → разбить на 2 файла
- `player/ParkourPlayer.java` (202) → разбить на 2 файла

### 1. ParkourGenerator.java → 3 файла
```
generator/
├── ParkourGenerator.java      (150) - координация
├── GeneratorState.java         (80)  - состояние (score, history)
└── GeneratorLifecycle.java     (80)  - tick, reset, fall
```

### 2. StorageSQL.java → 2 файла
```
storage/
├── SQLConnection.java          (120) - управление подключением
└── SQLRepository.java          (130) - CRUD операции
```

### 3. Session.java → 2 файла
```
session/
├── Session.java                (120) - основная логика
└── SessionMembers.java         (120) - управление участниками
```

### 4. ParkourUser.java → 2 файла
```
player/
├── ParkourUser.java            (120) - базовый user
└── UserScoreboard.java         (100) - scoreboard логика
```

### 5. ParkourPlayer.java → 2 файла
```
player/
├── ParkourPlayer.java          (120) - gameplay
└── PlayerSettings.java         (80)  - настройки
```

---

## 🎨 Новые фичи (Фаза 3-6)

### Визуальные темы
```java
public enum VisualTheme {
    NEON,      // неоновые блоки + яркие партиклы
    NATURE,    // трава, листья, цветы
    SPACE,     // космос, звёзды
    UNDERWATER,// вода, кораллы
    HELL       // нижний мир, лава
}
```

### Combo система
- Множитель очков за последовательные прыжки (x2, x3... x10)
- Визуальный индикатор combo
- Combo break при падении
- Бонусные очки за высокий combo

### Специальные прыжки
- **Neo-jump** - 4 блока по диагонали (5% шанс)
- **Head-hitter** - блок над головой (10% шанс)
- **Ladder jump** - прыжок на лестницу (15% шанс)

### Replay система
- Запись последнего прохождения
- Просмотр replay с камерой
- Экспорт/импорт replay файлов
- Замедление/ускорение воспроизведения

---

## 📅 Временная шкала (6-7 недель)

| Фаза | Задачи | Время |
|------|--------|-------|
| 0 | Критические баги | 1-2 дня |
| 1 | Рефакторинг файлов | 1 неделя |
| 2 | Исправление фич | 1 неделя |
| 3 | Новые режимы | 2 недели |
| 4 | Визуал (темы, combo) | 1 неделя |
| 5 | Ghost система | 3 дня |
| 6 | Replay система | 1 неделя |

---

## 🎯 Критерии релиза 2.0.0

- ✅ Все файлы < 200 строк
- ✅ 5+ режимов игры
- ✅ Ghost система активна
- ✅ Визуальные темы
- ✅ Combo система
- ✅ Replay система
- ✅ Специальные прыжки
