package dev.loki.loparkour.generator.jump;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JumpValidator.
 * Tests jump possibility calculations for parkour generation.
 */
class JumpValidatorTest {

    private JumpValidator validator;
    
    @Mock
    private World world;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new JumpValidator();
    }

    @Test
    void testCanJump_SameLevel_Distance1() {
        // 1 block forward, same height - always possible
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 1, 100, 0);
        
        assertTrue(validator.canJump(from, to), "1 block jump at same level should be possible");
    }

    @Test
    void testCanJump_SameLevel_Distance4() {
        // 4 blocks forward, same height - possible
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 4, 100, 0);
        
        assertTrue(validator.canJump(from, to), "4 block jump at same level should be possible");
    }

    @Test
    void testCanJump_SameLevel_Distance5() {
        // 5 blocks forward, same height - impossible without speed
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 5, 100, 0);
        
        assertFalse(validator.canJump(from, to), "5 block jump at same level should be impossible");
    }

    @Test
    void testCanJump_Up1_Distance3() {
        // 3 blocks forward, 1 block up - possible
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 3, 101, 0);
        
        assertTrue(validator.canJump(from, to), "3 block jump with 1 up should be possible");
    }

    @Test
    void testCanJump_Up1_Distance4() {
        // 4 blocks forward, 1 block up - impossible
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 4, 101, 0);
        
        assertFalse(validator.canJump(from, to), "4 block jump with 1 up should be impossible");
    }

    @Test
    void testCanJump_Down1_Distance4() {
        // 4 blocks forward, 1 block down - possible
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 4, 99, 0);
        
        assertTrue(validator.canJump(from, to), "4 block jump with 1 down should be possible");
    }

    @Test
    void testCanJump_Down2_Distance4() {
        // 4 blocks forward, 2 blocks down - possible
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 4, 98, 0);
        
        assertTrue(validator.canJump(from, to), "4 block jump with 2 down should be possible");
    }

    @Test
    void testCanJump_Up2_Distance1() {
        // 1 block forward, 2 blocks up - impossible (max jump height is 1.25)
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 1, 102, 0);
        
        assertFalse(validator.canJump(from, to), "Jump 2 blocks up should be impossible");
    }

    @Test
    void testCanJump_Diagonal() {
        // Diagonal jump - 3 blocks X, 2 blocks Z, same height
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 3, 100, 2);

        assertTrue(validator.canJump(from, to),
                "Diagonal jump of ~" + Math.sqrt(3 * 3 + 2 * 2) + " blocks should be possible");
    }

    @Test
    void testCanJump_NullLocations() {
        // Test null safety
        Location validLoc = new Location(world, 0, 100, 0);
        assertFalse(validator.canJump((Location)null, validLoc), 
                "Should return false for null from location");
        assertFalse(validator.canJump(validLoc, (Location)null), 
                "Should return false for null to location");
        assertFalse(validator.canJump((Location)null, (Location)null), 
                "Should return false for both null locations");
    }

    @Test
    void testCanJump_SameLocation() {
        // Jump to same location - should be false
        Location loc = new Location(world, 0, 100, 0);
        
        assertFalse(validator.canJump(loc, loc), "Jump to same location should be impossible");
    }

    @Test
    void testCanJump_VeryLongDistance() {
        // 10 blocks forward - impossible
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 10, 100, 0);
        
        assertFalse(validator.canJump(from, to), "10 block jump should be impossible");
    }

    @Test
    void testCanJump_FromBottomSlab_ReducedUpwardCapability() {
        // Test that bottom slabs reduce upward jump capability
        // This test uses mock objects, so the slab detection won't work,
        // but it documents the expected behavior
        Location from = new Location(world, 0, 100, 0);
        Location to = new Location(world, 2, 101, 0);
        
        // This should still pass with mock objects (no slab detection)
        assertTrue(validator.canJump(from, to), "2 block jump with 1 up should be possible from mock location");
    }
}
