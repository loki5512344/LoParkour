# LoParkour Bug Tracker

## CRITICAL

### 1. ProfileValue.asInt/asDouble — NumberFormatException при пустой строке
- **Файл:** `Profile.java:57-58`
- **Описание:** `Integer.parseInt(value)` и `Double.parseDouble(value)` падают при пустой строке `""`, которая возвращается по умолчанию при отсутствии ключа (`new ProfileValue("")`)
- **Фикс:** Добавить дефолтное значение либо кидать понятное исключение, а не NFE

### 2. Bukkit.createBlockData — IllegalArgumentException при невалидном materialName
- **Файл:** `GeneratorProfileManager.java:133`
- **Описание:** Если materialName из конфига невалидный (опечатка в generation.yml), `Bukkit.createBlockData()` кинет исключение
- **Фикс:** Валидировать materialName перед вызовом, либо ловить конкретно на строке 133

### 3. JumpDirector диагональный дрейф — heading никогда не выпрямляется
- **Файл:** `JumpDirector.java:87-99`
- **Описание:** `getRecommendedHeading()` всегда создает `Vector(1,0,1)` для X и Z границ. После нормализации heading становится диагональным и НИКОГДА не возвращается к осевому направлению
- **Фикс:** Использовать `Vector(1,0,0)` для X и `Vector(0,0,1)` для Z. Сбрасывать heading на исходный при отсутствии границы

### 4. subList().clear() на synchronizedList(LinkedList) — повреждение node связей
- **Файл:** `GeneratorCleanup.java:50-56`
- **Описание:** `history.subList(0, removeCount).clear()` на `Collections.synchronizedList(new LinkedList<>())` — compound операция не атомарна. ConcurrentModificationException или повреждение связей
- **Фикс:** Оборачивать в `synchronized(history)` или использовать CopyOnWriteArrayList

### 5. Мультиплеер — runaway генерация блоков
- **Файл:** `PlayerInteractionHandler.java:116-118` + `LifecycleTickManager.java:89-97`
- **Описание:** Каждый игрок триггерит `generate(1)` при скоре. N игроков = N блоков/тик. maintainBlockLead добавляет еще. Положительная обратная связь
- **Фикс:** Не больше 1 блока за тик независимо от числа игроков, кэп на maintainBlockLead

### 6. Система схематик полностью сломана
- **Файл:** `PlayerInteractionHandler.java:111-113` + `BlockPlacer.java:207-212`
- **Описание:** `isSchematicEndBlock` возвращает true для ЛЮБОГО schematic блока. При входе в схематик сразу удаляется + блоки засчитываются как очки
- **Фикс:** `isSchematicEndBlock` должен проверять только ПОСЛЕДНИЙ блок схематика. Исключить schematic блоки из скоринга

### 7. Прыжки за границами валидации
- **Файл:** `JumpCalculator.java:101-114`
- **Описание:** JumpOffsetGenerator генерирует offset до 5 блоков, total horizontal = sqrt(4²+5²)=6.4 при лимите 4.1. Retry-цикл не гарантирует валидность за 10 попыток
- **Фикс:** Ограничить randomOffset пределами валидатора: `maxRandomOffset = sqrt(maxHorizontal² - distance²)`

## HIGH

### 8. angleInY вращает offset относительно глобального Option.HEADING
- **Файл:** `JumpCalculator.java:105-113`
- **Описание:** rotateAroundY использует разницу между текущим heading и Option.HEADING, а не между текущим и предыдущим heading. Блоки уходят вбок
- **Фикс:** Вращать offset относительно предыдущего heading

### 9. Дублирование canJump(Location) и canJump(Vector) — рассинхронизация
- **Файл:** `JumpValidator.java:73-161`
- **Описание:** Два метода canJump. Location-версия учитывает slab-корректировку, Vector — нет
- **Фикс:** Убрать дублирование или добавить adjustment в Vector-версию

### 10. synchronizedList — итерации без синхронизации
- **Файл:** `GeneratorState.java:35`
- **Описание:** Итерации по `history` (cleanupDistantBlocks, historyContains, indexOfBlock) не обернуты в `synchronized`. Пока все на main thread, но это хрупко
- **Фикс:** Добавить `synchronized(history)` на все итерации, либо убрать synchronizedList

### 11. heading.clone() без null-проверки
- **Файл:** `JumpCalculator.java:101-103`
- **Описание:** `generator.state.heading.clone()` упадет с NPE, если heading не инициализирован до первого вызова calculateJumpOffset
- **Фикс:** Добавить null-guard: если heading == null, инициализировать из Option.HEADING

## MEDIUM

### 12. BlockSelector.selectBlockData рекурсия при пустом списке стилей
- **Файл:** `BlockSelector.java:32-33`
- **Описание:** Если style == null, код сбрасывает стиль на первый. `orElseThrow()` упадет, если список пуст
- **Фикс:** Проверка `if (styles.isEmpty()) return Material.STONE.createBlockData()`

### 13. checkPlayerFall — break вместо continue
- **Файл:** `LifecycleTickManager.java:74-78`
- **Описание:** При падении двух игроков в одном тике, второй пропускает обработку до следующего тика
- **Фикс:** Заменить break на continue

### 14. removeTrailBlocks сдвигает индексы истории
- **Файл:** `GeneratorCleanup.java:79-93`
- **Описание:** `history.subList(startIndex, endIndex).clear()` сдвигает индексы при параллельном вызове historyContains
- **Фикс:** Защититься копированием или проверять синхронизацию

### 15. Дублирование isSlabMaterial
- **Файл:** `JumpValidator.java:67-71`
- **Описание:** JumpValidator и BlockSelector имеют дублирующуюся логику определения slab. Рассинхронизация при изменениях
- **Фикс:** Вынести в общий утилитарный метод

### 16. DifficultyScore thresholds для льда (1.0/2.0) могут быть недостижимы
- **Файл:** `GeneratorProfileManager.java:57-69`
- **Описание:** packed_ice добавляется при difficulty >= 1.0, blue_ice при >= 2.0. Многие конфиги не достигают этих значений. Лед не появляется
- **Фикс:** Нормализовать difficulty до предсказуемой шкалы или изменить thresholds

### 17. schematicBlocks — ArrayList без thread safety
- **Файл:** `GeneratorState.java:41`
- **Описание:** schematicBlocks это plain ArrayList, без синхронизации, в отличие от history
- **Фикс:** Добавить синхронизацию или документировать main-thread-only

## LOW

### 18. scoredBlocks HashSet не очищается при fall
- **Файл:** `ParkourPlayer.java:125-127`
- **Описание:** HashSet растет бесконечно. При возрождении на тех же координатах не засчитает блоки
- **Фикс:** scoredBlocks.clear() при fall/reset

### 19. getFormattedTime() — Date(elapsed) хак
- **Файл:** `GeneratorStatistics.java:90-91`
- **Описание:** `new Date(elapsed)` — Date ожидает timestamp от 1970, а передается длительность. Сломается при >24ч
- **Фикс:** Использовать Duration API для форматирования

### 20. isUnsafeParkourMaterial блокирует PUMPKIN и _BED без причины
- **Файл:** `BlockSelector.java:48-60`
- **Описание:** PUMPKIN не имеет onPlace-сканирования соседей. _BED безопасны при setBlockData с флагом false
- **Фикс:** Убрать лишние исключения
