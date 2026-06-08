# LoParkour Bug Tracker

## ✅ Fixed (19/20)

### CRITICAL (7/7 fixed)

- ~~**1. ProfileValue.asInt/asDouble — NumberFormatException при пустой строке**~~ ✅
- ~~**2. Bukkit.createBlockData — IllegalArgumentException при невалидном materialName**~~ ✅
- ~~**3. JumpDirector диагональный дрейф — heading никогда не выпрямляется**~~ ✅
- ~~**4. subList().clear() на synchronizedList(LinkedList) — повреждение node связей**~~ ✅
- ~~**5. Мультиплеер — runaway генерация блоков**~~ ✅
- ~~**6. Система схематик полностью сломана**~~ ✅
- ~~**7. Прыжки за границами валидации**~~ ✅

### HIGH (4/4 fixed)

- ~~**8. angleInY вращает offset относительно глобального Option.HEADING**~~ ✅
- ~~**9. Дублирование canJump(Location) и canJump(Vector) — рассинхронизация**~~ ✅
- ~~**10. synchronizedList — итерации без синхронизации**~~ ✅
- ~~**11. heading.clone() без null-проверки**~~ ✅

### MEDIUM (5/6 fixed)

- ~~**12. BlockSelector.selectBlockData рекурсия при пустом списке стилей**~~ ✅
- ~~**13. checkPlayerFall — break вместо continue**~~ ✅
- ~~**14. removeTrailBlocks сдвигает индексы истории**~~ ✅
- ~~**15. Дублирование isSlabMaterial**~~ ✅
- ~~**16. DifficultyScore thresholds для льда (1.0/2.0)**~~ ✅
- **17. schematicBlocks — ArrayList без thread safety** — main-thread-only, неактуально

### LOW (3/3 fixed)

- ~~**18. scoredBlocks HashSet не очищается при fall**~~ ✅
- ~~**19. getFormattedTime() — Date(elapsed) хак**~~ ✅
- ~~**20. isUnsafeParkourMaterial блокирует PUMPKIN и _BED**~~ ✅

---

**Всего найдено: 20 | Исправлено: 19 | Пропущено: 1 (main-thread-only)**
