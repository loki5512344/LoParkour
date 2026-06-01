package dev.loki.loparkour.world;

import dev.loki.loparkour.world.core.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldNameValidationTest {

    @Test
    void allowsSafeFolderNames() {
        assertTrue(World.isValidWorldFolderName("parkour"));
        assertTrue(World.isValidWorldFolderName("lp_world"));
        assertTrue(World.isValidWorldFolderName("a1"));
    }

    @Test
    void rejectsPathTraversalAndSeparators() {
        assertFalse(World.isValidWorldFolderName("../etc"));
        assertFalse(World.isValidWorldFolderName("a/b"));
        assertFalse(World.isValidWorldFolderName("a\\b"));
    }

    @Test
    void rejectsBlankOrTooLong() {
        assertFalse(World.isValidWorldFolderName(""));
        assertFalse(World.isValidWorldFolderName("   "));
        assertFalse(World.isValidWorldFolderName("x".repeat(65)));
    }
}
