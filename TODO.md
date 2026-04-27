# LoParkour — TODO (обновлено 2026-04-27)

> Roadmap для превращения плагина в платформу с адаптивной генерацией.

## Легенда

- 🔴 Критично — блокирует работу
- 🟡 Важно — заявленная функциональность
- 🟢 Планово — улучшение
- ✅ Готово

---

## Фаза 1: Рефакторинг (2-3 дня)

### 🟢 Архитектура

| # | Задача | Файлы | Статус |
|---|--------|-------|--------|
| 1 | Разбить ParkourGenerator (335 строк) | Generator.java, GeneratorConfig.java | 🟢 |
| 2 | Разбить GeneratorProfileManager | ProfileManager.java, ProfileCalculator.java | 🟢 |
| 3 | Вынести statistics из generator | statistics/TimeTracker.java, ScoreTracker.java, LeaderboardSync.java | 🟢 |
| 4 | Создать структуру пакетов | generator/core/, generator/profile/, mode/base/, mode/impl/ | 🟢 |
| 5 | Рефакторинг интерфейсов | GameMode, DifficultyProfile, GeneratorEventListener | 🟢 |

### 🟢 Большие файлы (>200 строк)

| # | Файл | Строк | Действие | Статус |
|---|------|-------|----------|--------|
| 1 | Option.java | 335 | Разбить по типам опций | 🟢 |
| 2 | ElytraGenerator.java | 261 | Вынести в mode/elytra/ | 🟢 |
| 3 | Locales.java | 260 | Разбить на LocaleLoader + LocaleCache | 🟢 |
| 4 | ParkourPlayer.java | 236 | Вынести inventory/settings в отдельные классы | 🟢 |
| 5 | Leaderboard.java | 234 | Разбить на Leaderboard + LeaderboardStorage | 🟢 |

---

## Фаза 2: Адаптивная система (3-4 дня)

### 🟡 Инфраструктура

| # | Задача | Пакет | Статус |
|---|--------|-------|--------|
| 1 | Создать модели данных | adaptive/model/ | 🟡 |
| 2 | Реализовать хранилища | adaptive/storage/ | 🟡 |
| 3 | SQL таблицы для метрик | loparkour_player_stats | 🟡 |
| 4 | Файловое хранилище | playerdata/<uuid>.json | 🟡 |

### 🟡 Сбор метрик

| # | Задача | Класс | Статус |
|---|--------|-------|--------|
| 1 | MetricsCollector | adaptive/core/MetricsCollector.java | 🟡 |
| 2 | GeneratorEventListener | generator/core/GeneratorEventListener.java | 🟡 |
| 3 | Интеграция с Generator | Generator.score(), Generator.fall() | 🟡 |
| 4 | Детекция nearMiss | MetricsCollector.onBlockLand() | 🟡 |

### 🟡 Анализ и корректировка

| # | Задача | Класс | Статус |
|---|--------|-------|--------|
| 1 | SkillAnalyzer | adaptive/core/SkillAnalyzer.java | 🟡 |
| 2 | DifficultyCalculator | adaptive/core/DifficultyCalculator.java | 🟡 |
| 3 | DifficultyAdjuster | adaptive/core/DifficultyAdjuster.java | 🟡 |
| 4 | Формулы рейтинга | SkillRating calculation | 🟡 |

### 🟡 Интеграция с режимами

| # | Задача | Класс | Статус |
|---|--------|-------|--------|
| 1 | AdaptiveMode базовый класс | mode/base/AdaptiveMode.java | 🟡 |
| 2 | AdaptiveSession | adaptive/core/AdaptiveSession.java | 🟡 |
| 3 | StatsService | adaptive/core/StatsService.java | 🟡 |
| 4 | Миграция DefaultMode | mode/impl/DefaultMode.java | 🟡 |
| 5 | Миграция SpeedrunMode | mode/impl/SpeedrunMode.java, SpeedrunGenerator.java | 🟡 |
| 6 | Миграция HardcoreMode | mode/impl/HardcoreMode.java | 🟡 |
| 7 | Миграция GravityShiftMode | mode/impl/GravityShiftMode.java | 🟡 |

### 🟡 Конфигурация

| # | Задача | Файл | Статус |
|---|--------|------|--------|
| 1 | Секция adaptive в config.yml | config.yml | 🟡 |
| 2 | Конфиги режимов | modes/*.yml | 🟡 |
| 3 | AdaptiveConfig с кэшированием | adaptive/model/AdaptiveConfig.java | 🟡 |
| 4 | Предзагрузка при старте | LoParkour.onEnable() | 🟡 |

---

## Фаза 3: ElytraMode (1-2 дня)

### 🔴 Критичные классы (отсутствуют)

| # | Класс | Ответственность | Статус |
|---|-------|-----------------|--------|
| 1 | ElytraConfig | Загрузка настроек из modes/elytra.yml | 🔴 |
| 2 | ElytraRing | Модель кольца (центр, радиус, направление) | 🔴 |
| 3 | RingGenerator | Генерация колец по траектории | 🔴 |
| 4 | RingRenderer | Отрисовка частицами | 🔴 |
| 5 | ElytraPhysics | Проверка пролёта, буст от фейерверков | 🔴 |

### 🔴 Интеграция

| # | Задача | Статус |
|---|--------|--------|
| 1 | Создать ElytraMode | 🔴 |
| 2 | Выдача элитры и фейерверков | 🔴 |
| 3 | Тесты полёта | 🔴 |
| 4 | Конфиг modes/elytra.yml | 🔴 |

---

## Фаза 4: Новые режимы (2-3 дня)

### 🟢 RoguelikeMode

| # | Задача | Статус |
|---|--------|--------|
| 1 | Случайные модификаторы каждые N блоков | 🟢 |
| 2 | Выбор перков перед стартом | 🟢 |
| 3 | Риск-награда развилки | 🟢 |
| 4 | Балансировка | 🟢 |

### 🟢 Проверка существующих режимов

| # | Режим | Задача | Статус |
|---|-------|--------|--------|
| 1 | RaceMode | Проверить прогресс-бар и финиш | 🟢 |
| 2 | CoopMode | Проверить мультиплеер | 🟢 |
| 3 | DefaultMode | Тесты после рефакторинга | 🟢 |
| 4 | SpeedrunMode | Тесты таймеров блоков | 🟢 |

---

## Архитектурные решения

### Структура пакетов

```
src/main/java/dev/loki/loparkour/
├── adaptive/                    # Адаптивная система
│   ├── core/                    # MetricsCollector, SkillAnalyzer, DifficultyCalculator, DifficultyAdjuster
│   ├── model/                   # PlayerMetrics, SkillRating, AdaptiveConfig
│   └── storage/                 # StatsRepository, SQLStatsStorage, FileStatsStorage
├── statistics/                  # Вынесено из generator
│   ├── TimeTracker.java
│   ├── ScoreTracker.java
│   └── LeaderboardSync.java
├── generator/
│   ├── core/                    # Generator, GeneratorState, GeneratorConfig, Island
│   ├── profile/                 # DifficultyProfile, ProfileManager, ProfileCalculator
│   ├── jump/                    # JumpCalculator, JumpValidator, JumpDirector, BlockPlacer
│   └── lifecycle/               # GeneratorLifecycle, GeneratorCleanup
└── mode/
    ├── base/                    # GameMode, AdaptiveMode, ModeRegistry
    ├── impl/                    # DefaultMode, SpeedrunMode, HardcoreMode, GravityShiftMode, RoguelikeMode
    └── elytra/                  # ElytraMode, ElytraConfig, ElytraRing, RingGenerator, RingRenderer, ElytraPhysics
```

### Зависимости

```
adaptive → generator/profile (только через DifficultyProfile интерфейс)
generator → НЕ знает про adaptive (через GeneratorEventListener)
statistics → используется и generator, и adaptive
mode → использует generator + опционально adaptive
```

### Хранение данных

**SQL (критичные метрики):**
- `loparkour_player_stats`: skill_rating, sessions_count, total_jumps, longest_streak
- Автосохранение каждые 5 минут

**JSON (детальная аналитика):**
- `playerdata/<uuid>.json`: avgTimePerBlock, jumpTypeStats, nearMissCount
- `playerdata/history/<uuid>_jumps.jsonl`: история прыжков (последние 1000)
- Сохранение при выходе игрока

---

## Технический долг

### 🟢 Оптимизация

| # | Задача | Приоритет | Статус |
|---|--------|-----------|--------|
| 1 | Кэширование конфигов режимов | Средний | ✅ (в дизайне) |
| 2 | Batch updates для SQL метрик | Средний | 🟢 |
| 3 | Очистка старой истории прыжков | Низкий | 🟢 |

### 🟢 Тестирование

| # | Задача | Статус |
|---|--------|--------|
| 1 | Unit тесты для адаптивной системы | 🟢 |
| 2 | Integration тесты для режимов | 🟢 |
| 3 | Performance тесты (100+ игроков) | 🟢 |

---

## Чек-лист перед релизом

- [ ] Все режимы работают (default, speedrun, hardcore, gravity-shift, elytra)
- [ ] Адаптивная система корректно собирает метрики
- [ ] SQL миграции применены
- [ ] Конфиги валидируются при загрузке
- [ ] Нет memory leaks (проверить с VisualVM)
- [ ] Документация обновлена (README.md, CHANGELOG.md)
- [ ] Тесты проходят (`./gradlew test`)
- [ ] Build успешен (`./gradlew build`)

---

## Оценка времени

| Фаза | Задач | Дней | Начало | Конец |
|------|-------|------|--------|-------|
| Фаза 1: Рефакторинг | 10 | 2-3 | День 1 | День 3 |
| Фаза 2: Адаптивность | 20 | 3-4 | День 4 | День 7 |
| Фаза 3: ElytraMode | 9 | 1-2 | День 8 | День 9 |
| Фаза 4: Новые режимы | 8 | 2-3 | День 10 | День 12 |
| **Итого** | **47** | **8-12** | - | - |

---

## Метрики успеха

### Технические
- ✅ Все файлы < 200 строк
- ✅ Максимум 6 файлов в папке
- ✅ SOLID принципы соблюдены
- ✅ Нет циклических зависимостей

### Функциональные
- ✅ Адаптивная сложность работает для 4+ режимов
- ✅ Метрики сохраняются между сессиями
- ✅ ElytraMode полностью функционален
- ✅ Новый режим (Roguelike) добавлен

### Производительность
- ✅ TPS не падает при 50+ игроках
- ✅ SQL запросы < 50ms
- ✅ Конфиги кэшируются (нет I/O при входе игрока)

---

## SQL миграция

```sql
-- Создание таблицы для адаптивных метрик
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

-- Миграция существующих данных (если есть старая таблица)
INSERT INTO loparkour_player_stats (player_uuid, sessions_count, last_updated)
SELECT player_uuid, COUNT(*) as sessions, MAX(timestamp) as last_updated
FROM loparkour_scores
GROUP BY player_uuid
ON DUPLICATE KEY UPDATE sessions_count = VALUES(sessions_count);
```
