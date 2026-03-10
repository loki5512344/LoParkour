package dev.loki.loparkour.ghost;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GhostData serialization and deserialization.
 * Tests file I/O operations for ghost recordings.
 */
class GhostDataTest {

    @Test
    void testConstructor() {
        // Test basic construction
        List<GhostFrame> frames = new ArrayList<>();
        frames.add(new GhostFrame(0, 0, 100, 0, 0, 0));
        frames.add(new GhostFrame(1000, 1, 100, 0, 90, 0));
        
        GhostData data = new GhostData("TestPlayer", "uuid-123", 42, frames);
        
        assertEquals("TestPlayer", data.getPlayerName());
        assertEquals("uuid-123", data.getPlayerUUID());
        assertEquals(42, data.getScore());
        assertEquals(2, data.getFrames().size());
    }

    @Test
    void testSaveAndLoad(@TempDir Path tempDir) throws IOException {
        // Create test data
        List<GhostFrame> frames = new ArrayList<>();
        frames.add(new GhostFrame(0, 0, 100, 0, 0, 0));
        frames.add(new GhostFrame(500, 1, 100, 0, 45, 0));
        frames.add(new GhostFrame(1000, 2, 100, 0, 90, 0));
        
        GhostData original = new GhostData("Player1", "uuid-456", 100, frames);
        
        // Save to file
        File file = tempDir.resolve("test.ghost").toFile();
        original.saveToFile(file);
        
        assertTrue(file.exists(), "Ghost file should be created");
        assertTrue(file.length() > 0, "Ghost file should not be empty");
        
        // Load from file
        GhostData loaded = GhostData.loadFromFile(file);
        
        // Verify data
        assertEquals(original.getPlayerName(), loaded.getPlayerName());
        assertEquals(original.getPlayerUUID(), loaded.getPlayerUUID());
        assertEquals(original.getScore(), loaded.getScore());
        assertEquals(original.getFrames().size(), loaded.getFrames().size());
        
        // Verify frames
        for (int i = 0; i < original.getFrames().size(); i++) {
            GhostFrame origFrame = original.getFrames().get(i);
            GhostFrame loadedFrame = loaded.getFrames().get(i);
            
            assertEquals(origFrame.getTimestamp(), loadedFrame.getTimestamp());
            assertEquals(origFrame.getX(), loadedFrame.getX(), 0.001);
            assertEquals(origFrame.getY(), loadedFrame.getY(), 0.001);
            assertEquals(origFrame.getZ(), loadedFrame.getZ(), 0.001);
            assertEquals(origFrame.getYaw(), loadedFrame.getYaw(), 0.001);
            assertEquals(origFrame.getPitch(), loadedFrame.getPitch(), 0.001);
        }
    }

    @Test
    void testEmptyFrames(@TempDir Path tempDir) throws IOException {
        // Test with no frames
        GhostData data = new GhostData("EmptyPlayer", "uuid-789", 0, new ArrayList<>());
        
        File file = tempDir.resolve("empty.ghost").toFile();
        data.saveToFile(file);
        
        GhostData loaded = GhostData.loadFromFile(file);
        
        assertEquals(0, loaded.getFrames().size());
        assertEquals("EmptyPlayer", loaded.getPlayerName());
    }

    @Test
    void testLargeDataset(@TempDir Path tempDir) throws IOException {
        // Test with many frames (simulate 60 seconds at 20 TPS)
        List<GhostFrame> frames = new ArrayList<>();
        for (int i = 0; i < 1200; i++) {
            frames.add(new GhostFrame(
                i * 50L, // 50ms per frame
                i * 0.1, // X position
                100.0,   // Y position
                i * 0.05, // Z position
                (i % 360), // Yaw
                0 // Pitch
            ));
        }
        
        GhostData data = new GhostData("LongRunner", "uuid-long", 1200, frames);
        
        File file = tempDir.resolve("large.ghost").toFile();
        data.saveToFile(file);
        
        GhostData loaded = GhostData.loadFromFile(file);
        
        assertEquals(1200, loaded.getFrames().size());
        assertEquals(1200, loaded.getScore());
    }

    @Test
    void testSpecialCharactersInName(@TempDir Path tempDir) throws IOException {
        // Test with special characters in player name
        GhostData data = new GhostData(
            "Player_123-ABC", 
            "uuid-special", 
            50, 
            List.of(new GhostFrame(0, 0, 100, 0, 0, 0))
        );
        
        File file = tempDir.resolve("special.ghost").toFile();
        data.saveToFile(file);
        
        GhostData loaded = GhostData.loadFromFile(file);
        
        assertEquals("Player_123-ABC", loaded.getPlayerName());
    }

    @Test
    void testNegativeCoordinates(@TempDir Path tempDir) throws IOException {
        // Test with negative coordinates
        List<GhostFrame> frames = new ArrayList<>();
        frames.add(new GhostFrame(0, -100, 50, -200, -90, -45));
        frames.add(new GhostFrame(1000, -99, 51, -199, -45, 0));
        
        GhostData data = new GhostData("NegativePlayer", "uuid-neg", 10, frames);
        
        File file = tempDir.resolve("negative.ghost").toFile();
        data.saveToFile(file);
        
        GhostData loaded = GhostData.loadFromFile(file);
        
        GhostFrame frame = loaded.getFrames().get(0);
        assertEquals(-100, frame.getX(), 0.001);
        assertEquals(50, frame.getY(), 0.001);
        assertEquals(-200, frame.getZ(), 0.001);
        assertEquals(-90, frame.getYaw(), 0.001);
        assertEquals(-45, frame.getPitch(), 0.001);
    }

    @Test
    void testLoadNonExistentFile() {
        // Test loading from non-existent file
        File file = new File("nonexistent.ghost");
        
        assertThrows(IOException.class, () -> {
            GhostData.loadFromFile(file);
        }, "Should throw IOException for non-existent file");
    }

    @Test
    void testFramesImmutability() {
        // Test that frames list is copied (defensive copy)
        List<GhostFrame> originalFrames = new ArrayList<>();
        originalFrames.add(new GhostFrame(0, 0, 100, 0, 0, 0));
        
        GhostData data = new GhostData("Player", "uuid", 10, originalFrames);
        
        // Modify original list
        originalFrames.add(new GhostFrame(1000, 1, 100, 0, 0, 0));
        
        // GhostData should still have only 1 frame
        assertEquals(1, data.getFrames().size(), 
            "GhostData should create defensive copy of frames list");
    }
}
