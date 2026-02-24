package dev.loki.loparkour.schematic.lpschem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SchematicMarkers {

    private Vector3i start;
    private Vector3i end;
    private List<Vector3i> checkpoints;

    public SchematicMarkers(@NotNull Vector3i start, @NotNull Vector3i end) {
        this.start = start;
        this.end = end;
        this.checkpoints = new ArrayList<>();
    }

    public Vector3i getStart() {
        return start;
    }

    public void setStart(Vector3i start) {
        this.start = start;
    }

    public Vector3i getEnd() {
        return end;
    }

    public void setEnd(Vector3i end) {
        this.end = end;
    }

    public List<Vector3i> getCheckpoints() {
        return checkpoints;
    }

    public void addCheckpoint(Vector3i checkpoint) {
        checkpoints.add(checkpoint);
    }

    public static class Vector3i {
        public int x;
        public int y;
        public int z;

        public Vector3i(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
