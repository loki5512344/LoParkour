<div align="center">
<h3>LoParkour</h3>
<strong>
Advanced infinitely generating parkour plugin for Minecraft
<br>
by <a href="https://github.com/loki">loki</a>
</strong>
<br><br>

[![Version](https://img.shields.io/badge/version-1.3.1-blue.svg)](CHANGELOG.md)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.20.4+-green.svg)](https://papermc.io/)

</div>

## 📢 Latest Release - v1.3.1

**Critical security and stability update!**

- 🔒 **Fixed SQL Injection vulnerability** - All database operations now use PreparedStatement
- 🐛 **8 critical bugs fixed** - Ghost system, thread safety, data persistence
- ✅ **Config validation** - Prevents crashes from invalid configuration
- 🚀 **Performance improvements** - Optimized Random usage and player lookups

See [CHANGELOG.md](CHANGELOG.md) for full details.

## 🚀 Building

```bash
# Windows
gradlew.bat clean build

# Linux/Mac
./gradlew clean build
```

Output: `build/libs/LoParkour-1.3.0.jar`

## 📋 Requirements

- Java 21+
- Spigot/Paper 1.20.4+
- MySQL (optional, for leaderboards)

## ✨ Features

- **Infinite Generation** - Parkour generates endlessly as you play
- **Multiple Game Modes** - Default, Speedrun, Hardcore, Gravity Shift, Elytra
- **Ghost System** - Race against top player recordings
- **Custom Schematics** - Create and use your own parkour structures
- **Visual Themes** - Customize block styles and particle effects
- **Leaderboards** - Track high scores with MySQL or local storage
- **Rewards System** - Configure rewards for score milestones
- **Multi-language** - Full localization support

## 🎮 Game Modes

- **Default** - Classic infinite parkour
- **Speedrun** - Blocks disappear after a few seconds
- **Hardcore** - Lose all rewards on fall
- **Gravity Shift** - Random gravity effects (levitation, slow falling, jump boost)
- **Elytra** - Fly through particle rings

## 🗂️ Schematics

LoParkour uses `.lpschem` format for custom parkour structures:
- Lightweight JSON-based format with GZIP compression
- Supports block states, markers (start/end points), and metadata
- Automatic conversion from old vilib format: `/lp schematic convert`
- Create schematics in-game with `/lp schematic save <name>`
- Reload schematics: `/lp schematic reload`

## 🔧 Commands

- `/lp join` - Join parkour
- `/lp leave` - Leave parkour
- `/lp spectate <player>` - Spectate another player
- `/lp schematic <save|reload|convert>` - Manage schematics
- `/lp forcejoin <player>` - Force player to join (admin)
- `/lp reset` - Reset leaderboards (admin)

## 📊 Configuration

Main config: `plugins/LoParkour/config.yml`
- Enable/disable features
- Configure game modes
- Set up MySQL connection
- Customize generation settings
- Configure rewards

## 🌍 Localization

Add custom languages in `plugins/LoParkour/locales/`
- Default: English (`en.yml`)
- Included: Russian (`ru.yml`)
- Players can switch language in-game menu

## 🔌 Integrations

- **PlaceholderAPI** - Use parkour stats in other plugins
- **Vault** - Economy rewards support
- **HolographicDisplays** - Holographic leaderboards
- **Floodgate** - Bedrock player support

## 📝 Development

Built with LoLib 2.0 framework for modern Minecraft plugin development.

See `docs/CHANGES.md` for changelog.
