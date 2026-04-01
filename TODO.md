# LoParkour — TODO

> Обновлено: 2026-04-02. Длинный хвост старых аудитов и дублей: **[ARCHIVE_TODO.md](ARCHIVE_TODO.md)**.

## Легенда

- 🔴 Критично — баг/краш/безопасность
- 🟡 Важно — заявлено, но не работает / серьёзные проблемы
- 🟢 Планово — улучшение / техдолг
- ✅ Готово

---

## Главный план (стабилизация)

Консолидировано после обхода: меню/util, `LoParkour`, `LoParkourCommand`, join/teleport, `SessionStateManager`, `Island.build()`, Locales, LoLib 3.x.

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

### Контекст (не в «критичном» списке)

- Порядок `generateFirst` / телепорт из `Island.build()` — ок при неизменённом порядке вызовов.
- `history.contains(Block)` — Bukkit сравнивает по миру и координатам.
- `InventoryType.CHEST` в GUI — при странных кейсах проверить тип инвентаря.

### Порядок работ (регрессия)

1. `./gradlew build`, старт без `LinkageError`.
2. Ручной прогон паркура: счёт, меню, scoreboard, режимы.

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

## DuelMode

**Отдельная крупная фича** (не багфикс): PvP/дуэльный режим из старых заметок — проектировать и оценивать отдельно (сессии, арена, матчмейкинг, награды). Детали и старые идеи см. в **[ARCHIVE_TODO.md](ARCHIVE_TODO.md)** (поиск по «Duel» / режимам).

---

## Чек-лист перед релизом

- [ ] `./gradlew build`
- [ ] Старт сервера, `/parkour`, меню, один полный забег
- [ ] SQL: при проде выставить `LOPARKOUR_SQL_PASSWORD` на хосте
- [ ] Проверить миграцию старых `.dat` инвентарей при первом заходе игрока

---


