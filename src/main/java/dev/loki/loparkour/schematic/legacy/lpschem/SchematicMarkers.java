package dev.loki.loparkour.schematic.legacy.lpschem;

import java.util.List;

/** Gson POJO for legacy {@code .lpschem}. */
public class SchematicMarkers {

    public Vector3i start;
    public Vector3i end;
    public List<Vector3i> checkpoints;

    public static class Vector3i {
        public int x;
        public int y;
        public int z;
    }
}
