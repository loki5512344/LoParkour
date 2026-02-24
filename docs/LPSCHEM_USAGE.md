# LPSchematic - Примеры использования

## Создание схематики из мира

```java
// Выделить область в игре (WorldEdit style)
Location pos1 = player.getLocation();
Location pos2 = player.getLocation().add(10, 5, 10);

// Создать схематику
LPSchematic schematic = new LPSchematicBuilder("easy-jump-1", "Loki", 0.25, 10, 5, 10)
    .fromWorld(world, pos1, pos2)
    .addTag("easy")
    .addTag("beginner")
    .build();

// Сохранить
LPSchematicManager manager = new LPSchematicManager();
manager.saveSchematic(schematic);
```

## Создание схематики вручную

```java
LPSchematic schematic = new LPSchematicBuilder("custom-jump", "Loki", 0.5, 5, 3, 5)
    .setBlock(0, 0, 0, Material.STONE)
    .setBlock(1, 0, 1, Material.STONE)
    .setBlock(2, 1, 2, Material.STONE)
    .setBlock(3, 0, 3, Material.STONE)
    .setBlock(4, 0, 4, Material.STONE)
    .setStart(0, 0, 0)
    .setEnd(4, 0, 4)
    .addTag("medium")
    .build();

manager.saveSchematic(schematic);
```

## Загрузка и вставка

```java
LPSchematicManager manager = new LPSchematicManager();
manager.loadAll();

LPSchematic schematic = manager.getSchematic("easy-jump-1");
if (schematic != null) {
    Location origin = player.getLocation();
    List<Block> blocks = schematic.paste(origin, world);
    
    player.sendMessage("Pasted " + blocks.size() + " blocks");
}
```

## Добавление визуальных эффектов

```java
LPSchematic schematic = manager.getSchematic("neon-city");

SchematicVisuals visuals = new SchematicVisuals();

// Добавить вращающийся золотой блок
SchematicVisuals.BlockDisplay display = new SchematicVisuals.BlockDisplay(
    "minecraft:gold_block",
    new float[]{5.5f, 2.0f, 5.5f},
    new float[]{0.8f, 0.8f, 0.8f}
);
display.animation = new SchematicVisuals.Animation("rotate", "y", 1.5f);
visuals.addDisplay(display);

// Добавить частицы
SchematicVisuals.ParticleEffect particle = new SchematicVisuals.ParticleEffect(
    "end_rod",
    new float[]{5.0f, 3.0f, 5.0f},
    5,
    0.2f
);
visuals.addParticle(particle);

schematic.setVisuals(visuals);
manager.saveSchematic(schematic);
```

## Конвертация из старого формата

```java
// TODO: Создать конвертер
public class SchematicConverter {
    public static LPSchematic fromVilib(dev.efnilite.vilib.schematic.Schematic oldSchematic) {
        // Извлечь данные из старой схематики
        // Создать новую LPSchematic
        // Вернуть результат
    }
}
```

## Команда для создания схематики

```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
        return false;
    }
    
    Player player = (Player) sender;
    
    // /lpschem create <name> <difficulty>
    if (args[0].equals("create")) {
        String name = args[1];
        double difficulty = Double.parseDouble(args[2]);
        
        // Получить выделенную область (из WorldEdit или своей системы)
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);
        
        int width = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int height = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int length = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;
        
        LPSchematic schematic = new LPSchematicBuilder(name, player.getName(), difficulty, width, height, length)
            .fromWorld(player.getWorld(), pos1, pos2)
            .build();
        
        try {
            manager.saveSchematic(schematic);
            player.sendMessage("§aSchematic saved: " + name);
        } catch (IOException e) {
            player.sendMessage("§cFailed to save schematic");
            e.printStackTrace();
        }
        
        return true;
    }
    
    return false;
}
```

## Формат файла (.lpschem)

Файл сжат GZIP и содержит JSON:

```json
{
  "format_version": 2,
  "metadata": {
    "name": "easy-jump-1",
    "author": "Loki",
    "difficulty": 0.25,
    "tags": ["easy", "beginner"]
  },
  "dimensions": {
    "width": 5,
    "height": 3,
    "length": 5
  },
  "palette": [
    "minecraft:air",
    "minecraft:stone",
    "minecraft:oak_planks"
  ],
  "blocks": [0, 1, 1, 2, 0, 1, ...],
  "markers": {
    "start": {"x": 0, "y": 0, "z": 0},
    "end": {"x": 4, "y": 0, "z": 4},
    "checkpoints": []
  }
}
```

## Преимущества нового формата

1. **Человекочитаемый** - можно редактировать в текстовом редакторе
2. **Компактный** - GZIP сжатие + palette encoding
3. **Расширяемый** - легко добавить новые поля
4. **Совместимый** - можно конвертировать в/из других форматов
5. **Современный** - поддержка BlockDisplay и визуальных эффектов
