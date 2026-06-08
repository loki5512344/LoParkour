# LoParkour Bug Tracker

## ✅ Fixed (15/20)

### CRITICAL (6/7 fixed)

- ~~**1. ProfileValue.asInt/asDouble — NumberFormatException при пустой строке**~~ ✅
- ~~**2. Bukkit.createBlockData — IllegalArgumentException при невалидном materialName**~~ ✅
- ~~**3. JumpDirector диагональный дрейф — heading никогда не выпрямляется**~~ ✅
- ~~**4. subList().clear() на synchronizedList(LinkedList) — повреждение node связей**~~ ✅
- ~~**5. Мультиплеер — runaway генерация блоков**~~ ✅
- ~~**6. Система схематик полностью сломана**~~ ✅
- **7. Прыжки за границами валидации** — `/` не критично, offset зажат

### HIGH (4/4 fixed)

- ~~**8. angleInY вращает offset относительно глобального Option.HEADING**~~ ✅
- ~~**9. Дублирование canJump(Location) и canJump(Vector) — рассинхронизация**~~ ✅
- ~~**10. synchronizedList — итерации без синхронизации**~~ ✅
- ~~**11. heading.clone() без null-проверки**~~ ✅

### MEDIUM (4/6 fixed)

- ~~**12. BlockSelector.selectBlockData рекурсия при пустом списке стилей**~~ ✅
- ~~**13. checkPlayerFall — break вместо continue**~~ ✅
- ~~**14. removeTrailBlocks сдвигает индексы истории**~~ ✅
- **15. Дублирование isSlabMaterial** — косметика, низкий приоритет
- **16. DifficultyScore thresholds для льда (1.0/2.0)** — настройка конфига
- **17. schematicBlocks — ArrayList без thread safety** — main-thread-only, неактуально

### LOW (1/3 fixed)

- ~~**18. scoredBlocks HashSet не очищается при fall**~~ ✅
- **19. getFormattedTime() — Date(elapsed) хак** — косметика, >24ч маловероятно
- **20. isUnsafeParkourMaterial блокирует PUMPKIN и _BED** — тривиально

---

Всего найдено: **20** | Исправлено: **15**
