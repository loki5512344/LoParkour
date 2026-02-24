# Changelog - LoParkour

## Version 1.1.0 (Current)

### Added
- **Jump Validation System**
  - Mathematical validation using Pythagorean theorem
  - Maximum jump distance: 4.5 blocks
  - Configurable safe ranges for X, Y, Z axes
  - `JumpValidator` class for precise jump checking

- **Jump Types System**
  - NEO_JUMP - jumps around fences/walls
  - HEAD_HITTER - jumps with blocks overhead
  - FENCE_JUMP - jumps onto fences
  - TRAPDOOR_JUMP - jumps onto trapdoors
  - LADDER_JUMP - jumps onto ladders
  - Each type has difficulty multiplier
  - Configurable in config.yml

- **Memory Optimization**
  - Changed history storage from ArrayList to LinkedList
  - Automatic block cleanup at 20+ blocks distance
  - Periodic cleanup task every 100 ticks
  - Configurable cleanup settings

- **New Game Modes**
  - Speedrun Mode - blocks disappear after 1.5 seconds
  - Gravity Shift Mode - random potion effects every 10 jumps
  - Hardcore Mode - full reset on fall with optional reward loss

- **Ghost Mode System**
  - Records top-3 player movements per mode
  - Compression (only movements > 0.1 blocks)
  - Playback using ArmorStand entities
  - Binary format for efficient storage
  - Configurable ghost settings

- **Roguelike Perk System**
  - Double Jump perk (1 use)
  - Magnet perk (10 jumps, 20% distance reduction)
  - Second Chance perk (1 use, teleport back 3 blocks)
  - Shield perk (5 jumps, fall protection)
  - Rarity system: Common (60%), Rare (30%), Epic (10%)
  - Bonus blocks every 20 jumps

- **New .lpschem Schematic Format**
  - JSON + GZIP compression
  - Palette encoding for optimization
  - BlockData support (not just Material)
  - Auto-detect LIME_WOOL (start) and RED_WOOL (end)
  - Support for BlockDisplay entities
  - Support for particle effects
  - Ghost path data storage
  - Converter from old vilib format
  - New commands: save, paste, list, reload, convert

### Changed
- Migrated build system from Maven to Gradle
- Package renamed to dev.loki.loparkour
- Improved schematic command structure
- Updated all mode registrations

### Technical
- Java 17
- Spigot API 1.20.4
- Gradle 8.1.1
- Added Gson dependency for JSON serialization

---

## Version 1.0.0 (Legacy)

Initial release with basic parkour functionality.
