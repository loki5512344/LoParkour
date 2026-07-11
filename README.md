<div align="center">

# LoParkour

Advanced infinitely generating parkour plugin with ghost system, leaderboards and multiple game modes.

![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square&logo=openjdk&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-1.19.2+-blue?style=flat-square)
![Folia](https://img.shields.io/badge/Folia-supported-purple?style=flat-square)
![License](https://img.shields.io/badge/license-GPLv3-blue?style=flat-square&logo=gnu&logoColor=white)
![version](https://img.shields.io/badge/version-1.3.3-green?style=flat-square)

[English](#english) | [Русский](#russian)

</div>

---

<a name="english"></a>

## English

### Overview

LoParkour is an advanced parkour plugin with infinite world generation. The course generates endlessly as you play, with multiple game modes, a ghost racing system, custom schematics, visual themes, and MySQL leaderboards.

### Features

| Feature | Description |
|---------|-------------|
| Infinite generation | Parkour generates endlessly as you play |
| Multiple game modes | Default, Speedrun, Gravity Shift, Elytra |
| Ghost system | Race against top player recordings |
| Custom schematics | Create and use your own parkour structures (.lpschem) |
| Visual themes | Customize block styles and particle effects |
| Leaderboards | Track high scores with MySQL or local storage |
| Rewards system | Configure rewards for score milestones |
| Multi-language | Full localization support (en, ru included) |
| PlaceholderAPI | Parkour stats in other plugins |
| Vault economy | Economy rewards support |
| Floodgate | Bedrock player support |

### Game Modes

| Mode | Description |
|------|-------------|
| Default | Classic infinite parkour |
| Speedrun | Blocks disappear after a few seconds |
| Gravity Shift | Random gravity effects (levitation, slow falling, jump boost) |
| Elytra | Fly through particle rings |

### Commands

| Command | Description |
|---------|-------------|
| `/lp join` | Join parkour |
| `/lp leave` | Leave parkour |
| `/lp spectate <player>` | Spectate another player |
| `/lp schematic save <name>` | Save a schematic |
| `/lp schematic reload` | Reload schematics |
| `/lp schematic convert` | Convert old vilib format |
| `/lp forcejoin <player>` | Force player to join (admin) |
| `/lp reset` | Reset leaderboards (admin) |

### Dependencies

- Required: Paper 1.19.2+, Java 21+
- Optional: PlaceholderAPI, Vault, HolographicDisplays, Floodgate, Multiverse-Core

### Installation

1. Drop the jar into `plugins/`
2. Restart the server
3. Configure `plugins/LoParkour/config.yml`

---

<a name="russian"></a>

## Русский

### Обзор

LoParkour - продвинутый паркур-плагин с бесконечной генерацией мира. Трасса генерируется по мере игры, с несколькими режимами, системой призраков, кастомными схематиками, визуальными темами и MySQL лидербордами.

### Возможности

| Возможность | Описание |
|-------------|----------|
| Бесконечная генерация | Паркур генерируется по мере игры |
| Несколько режимов | Default, Speedrun, Gravity Shift, Elytra |
| Система призраков | Гонка против записей топ-игроков |
| Кастомные схематики | Создавайте свои структуры (.lpschem) |
| Визуальные темы | Настройка стилей блоков и частиц |
| Лидерборды | Топ результатов через MySQL или локально |
| Награды | Настройка наград за достижение очков |
| Мультиязычность | Полная локализация (en, ru) |
| PlaceholderAPI | Статистика паркура в других плагинах |
| Vault | Экономические награды |
| Floodgate | Поддержка бедрок-игроков |

### Команды

| Команда | Описание |
|---------|----------|
| `/lp join` | Начать паркур |
| `/lp leave` | Выйти из паркура |
| `/lp spectate <игрок>` | Наблюдать за игроком |
| `/lp schematic save <имя>` | Сохранить схематику |
| `/lp schematic reload` | Перезагрузить схематики |
| `/lp forcejoin <игрок>` | Заставить игрока начать (админ) |
| `/lp reset` | Сбросить лидерборды (админ) |

### Зависимости

- Обязательные: Paper 1.19.2+, Java 21+
- Опциональные: PlaceholderAPI, Vault, HolographicDisplays, Floodgate, Multiverse-Core

### Установка

1. Положите jar в папку `plugins/`
2. Перезапустите сервер
3. Настройте `plugins/LoParkour/config.yml`

---

### Links

- [Releases](../../releases)
- [Issues](../../issues)
- [License](LICENSE)

### License

GNU General Public License v3.0