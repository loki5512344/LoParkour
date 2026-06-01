package dev.loki.loparkour.schematic.legacy.lpschem;

/** Gson POJO for legacy {@code .lpschem}. */
public class SchematicMetadata {

    private String name;
    private String author;
    private double difficulty;

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public double getDifficulty() {
        return difficulty;
    }
}
