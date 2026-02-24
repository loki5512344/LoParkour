package dev.loki.loparkour.schematic.lpschem;

public class SchematicDimensions {

    public int width;
    public int height;
    public int length;

    public SchematicDimensions(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    public int getTotalBlocks() {
        return width * height * length;
    }
}
