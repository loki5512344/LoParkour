package dev.loki.loparkour.generator;

import dev.loki.loparkour.api.Registry;
import dev.loki.loparkour.config.Option;
import dev.loki.loparkour.style.Style;
import dev.loki.loparkour.util.Probs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects next parkour blocks (SRP - Single Responsibility)
 */
public class BlockSelector {
    
    private final Profile profile;
    private final Location[] zone;
    private final Vector initialHeading;
    private final Map<Integer, Double> heightChances = new HashMap<>();
    private final Map<Integer, Double> distanceChances = new HashMap<>();
    
    private Vector heading;
    
    public BlockSelector(Profile profile, Location[] zone, Vector initialHeading) {
        this.profile = profile;
        this.zone = zone;
        this.heading = initialHeading.clone();
        this.initialHeading = initialHeading.clone();
        
        calculateChances();
    }
    
    private void calculateChances() {
        heightChances.put(1, Option.NORMAL_HEIGHT_1);
        heightChances.put(0, Option.NORMAL_HEIGHT_0);
        heightChances.put(-1, Option.NORMAL_HEIGHT_NEG1);
        heightChances.put(-2, Option.NORMAL_HEIGHT_NEG2);

        distanceChances.put(1, Option.NORMAL_DISTANCE_1);
        distanceChances.put(2, Option.NORMAL_DISTANCE_2);
        distanceChances.put(3, Option.NORMAL_DISTANCE_3);
        distanceChances.put(4, Option.NORMAL_DISTANCE_4);
    }
    
    public List<Block> selectBlocks(Block latest) {
        int height = Probs.random(heightChances);
        int distance = Probs.random(distanceChances);
        return List.of(selectNext(latest, distance, height));
    }
    
    private Block selectNext(Block current, int distance, int height) {
        JumpDirector director = new JumpDirector(
            BoundingBox.of(zone[0], zone[1]), 
            current.getLocation().toVector()
        );

        heading = director.getRecommendedHeading(heading);
        height = director.getRecommendedHeight(height);

        // Ensure special blocks are possible
        switch (current.getType()) {
            case SMOOTH_QUARTZ_SLAB -> height = Math.min(height, 0);
            case GLASS_PANE -> distance = Math.min(distance, 3);
        }

        if (height > 0) {
            distance = Math.max(distance - height, 1);
        }

        int randomOffset = new JumpOffsetGenerator(height, distance).getRandomOffset(0, 1);

        Vector offset = heading.clone()
                .multiply(distance + 1)
                .setY(height);
                
        if (offset.getX() == 0) {
            offset.setX(randomOffset);
        } else {
            offset.setZ(randomOffset);
        }

        offset.rotateAroundY(angleInY(heading, initialHeading));

        return current.getLocation().add(offset).getBlock();
    }
    
    public BlockData selectBlockData() {
        Style style = Registry.getStyle(profile.get("style").value());

        if (style == null) {
            profile.set("style", Registry.getStyles().stream()
                    .findFirst()
                    .orElseThrow()
                    .getName());
            return selectBlockData();
        }

        return style.getNext().createBlockData();
    }
    
    private double angleInY(Vector a, Vector b) {
        double det = a.getX() * b.getZ() - a.getZ() * b.getX();
        return Math.atan2(det, a.dot(b));
    }
    
    public Vector getHeading() {
        return heading;
    }
    
    public void resetHeading() {
        heading = initialHeading.clone();
    }
}
