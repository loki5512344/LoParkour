package dev.loki.loparkour.generator.core.coordinator;

import dev.loki.loparkour.generator.jump.calculation.JumpType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for listening to generator events.
 * Allows adaptive system to integrate without creating direct dependencies.
 *
 * This decouples the generator from the adaptive system:
 * - Generator fires events through this interface
 * - Adaptive system implements this interface to collect metrics
 * - No circular dependencies between generator and adaptive packages
 */
public interface GeneratorEventListener {

    /**
     * Called when a block is generated.
     *
     * @param player The player for whom the block was generated
     * @param block The generated block
     * @param jumpType The type of jump required to reach this block
     * @param distance The distance from the previous block
     */
    void onBlockGenerated(@NotNull Player player, @NotNull Block block, @NotNull JumpType jumpType, double distance);

    /**
     * Called when a player successfully lands on a block and scores.
     *
     * @param player The player who scored
     * @param block The block the player landed on
     * @param distance The distance of the jump
     */
    void onPlayerScore(@NotNull Player player, @NotNull Block block, double distance);

    /**
     * Called when a player falls off the parkour.
     *
     * @param player The player who fell
     * @param lastBlock The last block the player was on (may be null)
     */
    void onPlayerFall(@NotNull Player player, Block lastBlock);

    /**
     * Called when a player performs a jump.
     *
     * @param player The player who jumped
     * @param jumpType The type of jump performed
     */
    void onJump(@NotNull Player player, @NotNull JumpType jumpType);
}
