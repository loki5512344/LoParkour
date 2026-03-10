# LoParkour — TODO

> Обновлено: 2026-03-07

## Легенда
- 🔴 Критично — баг/краш/не компилируется
- 🟡 Важно — фича заявлена но не работает
- 🟢 Планово — улучшение
- ✅ Готово

---

## 🔴 Критично — из code review

| # | Баг | Файл | Что сделать |
|---|-----|------|-------------|
| 1 | ~~Ghost файлы по имени игрока~~ | `GhostManager.java`, `GhostData.java` | ✅ Используется UUID вместо имени |
| 2 | ~~`GhostRecorder.stopRecording()` не передаёт UUID~~ | `GhostRecorder.java`, `GeneratorLifecycle.java` | ✅ Добавлен параметр UUID |
| 3 | ~~`new Random()` в горячих методах~~ | `BlockPlacer.java` | ✅ Заменено на `ThreadLocalRandom.current()` |
| 4 | ~~`ConcurrentModificationException` в `reset()`~~ | `ParkourGenerator.java` | ✅ Синхронизация через копирование списка |
| 5 | ~~Сохраняется только DEFAULT лидерборд~~ | `LoParkour.java` disable() | ✅ Сохраняются все режимы (Speedrun, Race, Coop, Elytra) |
| 6 | ~~`cleanupDistantBlocks()` вызывается дважды~~ | `GeneratorLifecycle.java` | ✅ Убран дублирующий вызов |
| 7 | ~~`System.err.println` в JumpOffsetGenerator~~ | `JumpOffsetGenerator.java` | ✅ Используется Logger |
| 8 | ~~`disable()` глотает все исключения~~ | `LoParkour.java` | ✅ Логируются ошибки |
| 9 | ~~Первый блок может быть специальным~~ | `BlockPlacer.java` | ✅ Всегда полный блок из конфига |

---

## 🟡 Важно — улучшения безопасности

| # | Проблема | Файл | Что сделать |
|---|----------|------|-------------|
| 9 | ~~Отсутствует rate limiting~~ | `PlayerCommandHandler.java` | ✅ Cooldown уже реализован (2.5 сек на join/leave) |
| 10 | ~~Нет валидации конфигурации~~ | `Config.java` | ✅ Добавлена валидация критичных значений |
| 11 | ~~`loadIslandSchematic()` возвращает null~~ | `ParkourGenerator.java` | ✅ Fallback на дефолтный схематик "island" |
| 12 | ~~Hardcoded строки вместо локализации~~ | `MainMenu.java`, `CoopMode.java`, `RaceMode.java`, `ElytraMode.java` | ✅ Используется `Locales.getString()` |

---

## 🟢 Планово — оптимизация

| # | Улучшение | Файл | Что сделать |
|---|-----------|------|-------------|
| 13 | ~~`getPlayers()` создаёт новый список каждый раз~~ | `ParkourPlayer.java` | ✅ Оптимизирован `getPlayer()` без создания списка |
| 14 | ~~Магические числа~~ | `BlockPlacer.java`, `GeneratorLifecycle.java` | ✅ Вынесены в константы (MAX_RETRIES, FALL_DISTANCE, etc) |
| 15 | ~~`selectJumpType()` хардкод шансов~~ | `BlockPlacer.java` | ✅ Вынесено в generation.yml |
| 16 | ~~SQL Injection в writeScores/writePlayer~~ | `StorageSQL.java` | ✅ Переписано на PreparedStatement с параметрами |

---

## 🎉 Итоги рефакторинга

### Исправлено критичных багов: 8/8 ✅
- Ghost UUID параметр
- ThreadLocalRandom вместо new Random()
- ConcurrentModificationException
- Сохранение всех лидербордов
- Двойной вызов cleanup
- System.err → Logger
- Логирование ошибок в disable()
- **SQL Injection полностью устранён**

### Улучшения безопасности: 4/4 ✅
- Rate limiting (уже был)
- Валидация конфигурации
- Fallback для схематиков
- Локализация всех строк

### Оптимизация: 4/4 ✅
- Оптимизация getPlayer()
- Константы вместо магических чисел
- Конфигурируемые шансы jump types
- PreparedStatement для SQL

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

## ✅ Уже исправлено

- ✅ BOM в `ParkourGenerator.java` ломает компиляцию
- ✅ `generator.score` → `generator.state.score`
- ✅ `ParkourUser.joinCount` удалён
- ✅ `PlayerSettingsManager.OptionContainer` не public
- ✅ `Option.PARTICLE_DATA.data()` не существует
- ✅ `ParticleUtil.circle()` неправильные параметры
- ✅ `PotionEffectType.JUMP` и `SLOW` удалены в Paper 1.20.5+
- ✅ `ElytraMode` создаёт обычный `ParkourGenerator`
- ✅ `SpeedrunMode`: таймеры `runLater` продолжают тикать после `reset()`
- ✅ `HardcoreMode`: `collectedRewards` не очищается при падении
- ✅ `JumpValidator` создан но нигде не вызывается
- ✅ `JumpType` enum создан но не интегрирован в генератор
- ✅ `ConfigUpdater` закомментирован
- ✅ `GravityShiftMode`: нет визуала при смене эффекта
- ✅ Ghost система не интегрирована
- ✅ Удалить мёртвые stub-файлы
- ✅ ElytraMode: кольца из партиклов + детекция пролёта
- ✅ ElytraMode: cooldown на фейерверки
- ✅ RaceMode
- ✅ CoopMode
- ✅ Реорганизация `generator/` по подпакетам
- ✅ SQL Injection исправлен (PreparedStatement)
- ✅ PreparedStatement leak исправлен (try-with-resources)
- ✅ Scheduler Task Leak в SpeedrunMode исправлен
- ✅ Удалены GravityShiftMode и HardcoreMode (запутанные режимы)
- ✅ Предметы в хотбаре вместо инвентаря
- ✅ Схематики отключены из генерации
- ✅ Сложность генерации сбалансирована
- ✅ Ограничения после специальных блоков (слэбы, заборы, панели)
- ✅ Clamp значений height/distance для безопасности
- ✅ Fallback в JumpOffsetGenerator
- ✅ Локали для race и coop режимов
- ✅ Конфиг для race и coop режимов
- ✅ Fallback для first-block-material в Island.java
- ✅ Ghost файлы по UUID вместо имени (частично - нужно обновить вызовы)

---

## 🎮 Чек-лист перед релизом

- [x] `./gradlew build` без ошибок
- [x] Все критичные баги исправлены (пункты 1-8)
- [x] Все улучшения безопасности (пункты 9-12)
- [x] Все оптимизации (пункты 13-16)
- [x] SQL Injection полностью устранён
- [x] Версия обновлена до 1.3.1
- [x] CHANGELOG.md создан
- [x] README.md обновлён
- [x] RELEASE_NOTES.md создан
- [x] Юнит-тесты добавлены (JumpValidator, GhostData)
- [x] Баги найденные тестами исправлены
- [x] Все тесты проходят успешно
- [ ] Запуск сервера без ошибок в консоли
- [ ] Паркур: прыжки, счёт, рекорд работают
- [ ] Scoreboard обновляется
- [ ] Все меню открываются
- [ ] SQL: данные сохраняются и читаются
- [ ] Партиклы и звук в каждом режиме
- [ ] Все режимы запускаются без ошибок (Default, Speedrun, Elytra, Race, Coop)
- [ ] Ghost система работает (запись и воспроизведение)
- [ ] Нет невозможных прыжков после специальных блоков
- [ ] Предметы в хотбаре (слоты 1,3,4,5,7)

---

## 📊 Оценка из code review

**Общая оценка: 7.5/10**

**Сильные стороны:**
- Отличная структура пакетов
- Есть API для разработчиков
- Хорошая модульность (режимы, генератор, хранилище)
- Много систем (leaderboard, ghost, generator, schematics, menus, rewards)


**Что нужно исправить:**
- Критичные баги из таблицы выше
- Безопасность (rate limiting, валидация)
- Оптимизация (кеширование, константы)

**Потенциал:**
Проект уровня реального Spigot ресурса (15-25k строк кода). При исправлении критичных багов можно выпускать в production.
