package dev.loki.loparkour.schematic.lpschem;

import java.util.ArrayList;
import java.util.List;

public class SchematicLogic {

    private List<int[]> ghost_path;

    public SchematicLogic() {
        this.ghost_path = new ArrayList<>();
    }

    public List<int[]> getGhostPath() {
        return ghost_path;
    }

    public void setGhostPath(List<int[]> ghost_path) {
        this.ghost_path = ghost_path;
    }

    public void addGhostPoint(int x, int y, int z) {
        ghost_path.add(new int[]{x, y, z});
    }
}
