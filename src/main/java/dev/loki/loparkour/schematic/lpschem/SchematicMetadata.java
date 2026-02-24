package dev.loki.loparkour.schematic.lpschem;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class SchematicMetadata {
    private String name;
    private String author;
    private double difficulty;
    private List<String> tags;

    public SchematicMetadata(@NotNull String name, @NotNull String author, double difficulty) {
        this.name = name;
        this.author = author;
        this.difficulty = difficulty;
        this.tags = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public List<String> getTags() {
        return tags;
    }

    public void addTag(@NotNull String tag) {
        tags.add(tag);
    }
}