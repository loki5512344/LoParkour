# План миграции с vilib на LoLib 2.0

## 📋 Текущее состояние

### Используется из vilib:
1. **ViPlugin** - базовый класс плагина
2. **Logging** - логирование
3. **Strings.colour()** - форматирование цветов (MiniMessage + Legacy)
4. **FastBoard** - scoreboard обертка
5. **Menu/PagedMenu** - GUI система
6. **Item** - ItemBuilder
7. **Task** - планировщик задач
8. **Schematic** (старый) - система схематик
9. **ConfigUpdater** - обновление конфигов
10. **Particles** - particle эффекты
11. **Locations** - утилиты для Location

### Уже реализовано в LoParkour:
1. **LPSchematic** - новый формат схематик (.lpschem)
2. **LPSchematicBuilder** - builder для создания
3. **LPSchematicManager** - менеджер схематик
4. **SchematicConverter** - конвертер из старого формата

## 🎯 План миграции

### Этап 1: Подготовка (СЕЙЧАС)
- [x] Добавить api_docs в .gitignore
- [ ] Добавить lolib-2.0.0.jar в зависимости
- [ ] Удалить loparkour-vilib из проекта
- [ ] Обновить build.gradle

### Этап 2: Core миграция
- [ ] **ViPlugin → LoPlugin**
  - Заменить `extends ViPlugin` на `extends LoPlugin`
  - Изменить `enable()` и `disable()` методы
  - Добавить `dependencies()` метод если нужно

- [ ] **Logging → LoLogger**
  - Заменить `Logging logging` на использование `loLogger()`
  - Обновить все вызовы логирования

- [ ] **Task → Scheduler**
  - Заменить `Task.create()` на `Scheduler.get(plugin)`
  - Обновить async/sync задачи
  - Добавить поддержку Folia если нужно

### Этап 3: Utils миграция
- [ ] **Strings.colour() → Colors.parse()**
  - Уже есть конвертер `&#RRGGBB` → `<#RRGGBB>`
  - Заменить все вызовы `Strings.colour()` на `Colors.parse()`

- [ ] **Locations утилиты**
  - Перенести нужные методы или использовать LoLib аналоги

### Этап 4: GUI миграция
- [ ] **Menu → InventoryGUI**
  - Переписать все меню на новый API
  - Обновить PagedMenu на новую систему
  - Сохранить функциональность

- [ ] **Item → ItemBuilder**
  - Заменить vilib Item на LoLib ItemBuilder
  - Обновить создание предметов
  - Использовать DataComponent API для 1.21+

### Этап 5: Schematics миграция (ПРИОРИТЕТ!)
- [ ] **Удалить старую систему vilib**
  - Удалить импорты `dev.efnilite.vilib.schematic.*`
  - Удалить SchematicConverter (уже не нужен)

- [ ] **Интегрировать LoLib Schematics**
  - Использовать `dev.lolib.schematics.Schematic`
  - Использовать `dev.lolib.schematics.SchematicManager`
  - Обновить LPSchematic для совместимости

- [ ] **Обновить генератор**
  - Изменить `ParkourGenerator` для использования LoLib схематик
  - Обновить `Island` класс
  - Тестировать вставку схематик

### Этап 6: Дополнительные модули
- [ ] **FastBoard → Scoreboard**
  - Заменить FastBoard на LoLib Scoreboard API
  - Обновить ParkourUser.board

- [ ] **Particles**
  - Заменить vilib Particles на LoLib ParticleBuilder
  - Обновить эффекты в генераторе

- [ ] **ConfigUpdater**
  - Использовать LoLib Config API
  - Или оставить текущую реализацию

### Этап 7: Тестирование
- [ ] Проверить компиляцию
- [ ] Тестировать основной функционал
- [ ] Тестировать схематики
- [ ] Тестировать GUI
- [ ] Тестировать все режимы

## 📦 Зависимости

### Удалить:
```kotlin
// loparkour-vilib - удалить весь модуль
```

### Добавить:
```kotlin
dependencies {
    // LoLib Core modules
    implementation(files("libs/lolib-2.0.0.jar"))
    
    // Или если будет в Maven:
    // implementation("dev.lolib:lolib-core:2.0.0")
    // implementation("dev.lolib:lolib-scheduler:2.0.0")
    // implementation("dev.lolib:lolib-schematics:2.0.0")
    // implementation("dev.lolib:lolib-gui:2.0.0")
}
```

## 🔄 Маппинг API

### Core
| vilib | LoLib |
|-------|-------|
| `ViPlugin` | `LoPlugin` |
| `Logging` | `loLogger()` |
| `Task` | `Scheduler` |

### Utils
| vilib | LoLib |
|-------|-------|
| `Strings.colour()` | `Colors.parse()` |
| `Locations.*` | Встроенные методы или кастом |

### GUI
| vilib | LoLib |
|-------|-------|
| `Menu` | `InventoryGUI` |
| `PagedMenu` | `InventoryGUI` с пагинацией |
| `Item` | `ItemBuilder` |

### Schematics
| vilib | LoLib |
|-------|-------|
| `Schematic` | `dev.lolib.schematics.Schematic` |
| `Schematics` | `SchematicManager` |
| `SchematicReader/Writer` | Встроено в Schematic |

### Scoreboard
| vilib | LoLib |
|-------|-------|
| `FastBoard` | LoLib Scoreboard API |

## 🎨 Преимущества LoLib

1. **Современный API** - DataComponent вместо ItemMeta
2. **Folia support** - готовность к многопоточности
3. **Лучшая производительность** - оптимизированные алгоритмы
4. **Больше функций** - Database, Performance, Dialog API
5. **Активная разработка** - регулярные обновления
6. **Документация** - полная документация на русском

## 📝 Приоритеты

### Высокий приоритет:
1. Core миграция (LoPlugin, Logger, Scheduler)
2. Schematics миграция (критично для функционала)
3. GUI миграция (меню - основной интерфейс)

### Средний приоритет:
4. Utils миграция (Colors, форматирование)
5. Scoreboard миграция
6. Particles миграция

### Низкий приоритет:
7. Config API (можно оставить текущий)
8. Дополнительные утилиты

## 🚀 Следующие шаги

1. Удалить loparkour-vilib папку
2. Обновить build.gradle.kts
3. Начать с Core миграции
4. Постепенно мигрировать остальные модули
5. Тестировать после каждого этапа

## ⚠️ Важные замечания

- **Не удалять LPSchematic** - это наш кастомный формат, совместимый с LoLib
- **Сохранить SchematicVisuals** - кастомная функциональность
- **Тестировать схематики** - критично для паркура
- **Backup перед миграцией** - на всякий случай

---

**Статус:** Готов к началу миграции  
**Дата:** 2026-02-25  
**Версия:** 1.2.4 → 1.3.0
