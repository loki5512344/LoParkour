package dev.loki.loparkour.schematic.core;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import dev.loki.loparkour.schematic.nbt.StructurePaster;
import dev.loki.loparkour.schematic.schem.SchemPaster;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A parkour jump structure from {@code .nbt} (vanilla) or {@code .schem} / {@code .schematic} (WorldEdit).
 */
public final class ParkourSchematic {

    private final String id;
    private final double difficulty;
    private final SchematicFormat format;
    @Nullable
    private final Structure nbtStructure;
    @Nullable
    private final Clipboard weClipboard;

    private ParkourSchematic(
            @NotNull String id,
            double difficulty,
            @NotNull SchematicFormat format,
            @Nullable Structure nbtStructure,
            @Nullable Clipboard weClipboard
    ) {
        this.id = id;
        this.difficulty = difficulty;
        this.format = format;
        this.nbtStructure = nbtStructure;
        this.weClipboard = weClipboard;
    }

    @NotNull
    public static ParkourSchematic fromNbt(@NotNull String id, double difficulty, @NotNull Structure structure) {
        return new ParkourSchematic(id, difficulty, SchematicFormat.NBT, structure, null);
    }

    @NotNull
    public static ParkourSchematic fromSchem(@NotNull String id, double difficulty, @NotNull Clipboard clipboard) {
        return new ParkourSchematic(id, difficulty, SchematicFormat.SCHEM, null, clipboard);
    }

    @NotNull
    public String getId() {
        return id;
    }

    public double getDifficulty() {
        return difficulty;
    }

    @NotNull
    public SchematicFormat getFormat() {
        return format;
    }

    @NotNull
    public List<Block> paste(@NotNull Location origin, @NotNull World world) {
        if (format == SchematicFormat.NBT && nbtStructure != null) {
            return StructurePaster.paste(nbtStructure, origin, world);
        }
        if (format == SchematicFormat.SCHEM && weClipboard != null) {
            return SchemPaster.paste(weClipboard, origin, world);
        }
        return List.of();
    }
}
