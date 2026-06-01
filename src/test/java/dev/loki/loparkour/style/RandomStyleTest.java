package dev.loki.loparkour.style;

import dev.loki.loparkour.style.core.RandomStyle;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RandomStyleTest {

    @Test
    void emptyMaterialListFallsBackToStone() {
        RandomStyle style = new RandomStyle("test", Collections.emptyList());
        assertEquals(Material.STONE, style.getNext());
    }

    @Test
    void copiesListAndReturnsOnlyDeclaredMaterials() {
        RandomStyle style = new RandomStyle("x", List.of(Material.OAK_PLANKS));
        assertSame(Material.OAK_PLANKS, style.getNext());
    }
}
