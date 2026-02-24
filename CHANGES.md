# Changelog

## Version 6.0.0 (2026-02-24)

### 🔄 Major Changes
- **Полная переработка проекта**
  - Название: LoParkour
  - Package: `dev.loki.loparkour`
  - Главный класс: `LoParkour`
  - Автор: loki

### 🏗️ Build System
- **Миграция на Gradle**
  - Удалена Maven сборка (pom.xml)
  - Добавлен Gradle с Wrapper
  - Оптимизирована конфигурация зависимостей
  - Улучшена скорость сборки

### 📦 Technical
- Обновлена версия PlaceholderAPI: 2.11.2 → 2.11.6
- Исключена проблемная зависимость ConfigUpdater
- Настроен Shadow plugin для shading
- Добавлен .gitignore для Gradle

### 🎯 Commands
- Новая команда: `/loparkour` (aliases: `/parkour`, `/lp`)
- Старая команда `/witp` удалена

### 📝 Documentation
- Создан TODO.md с детальным планом развития
- Обновлен README.md
- Добавлены инструкции по сборке

---

## Version 1.0.0 and earlier

Initial development versions.
