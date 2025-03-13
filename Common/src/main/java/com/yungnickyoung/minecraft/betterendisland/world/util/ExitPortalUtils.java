package com.yungnickyoung.minecraft.betterendisland.world.util;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.mixin.accessor.EndDragonFightAccessor;
import com.yungnickyoung.minecraft.betterendisland.world.IBetterDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndPodiumFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class ExitPortalUtils {
    /**
     * Spawns the exit portal at the specified location, ensuring it is at a valid position
     * @param dragonFight The dragon fight instance
     * @param serverLevel The server level
     * @param isActive Whether the portal is active
     * @param isBottomOnly Whether only the bottom half of the portal should be spawned.
     *                     This is used in non-first exit portal spawns to prevent the top half from being replaced.
     */
    public static void spawnPortal(IBetterDragonFight dragonFight, ServerLevel serverLevel,
                                   boolean isActive, boolean isBottomOnly) {
        // TODO - add option for disabling the central tower and using vanilla instead?

        EndDragonFightAccessor fightAccessor = (EndDragonFightAccessor) dragonFight;

        // Update the portal location to ensure it is set to the correct position
        fightAccessor.setPortalLocation(getAdjustedPortalPos(fightAccessor.getPortalLocation(), serverLevel));

        // Spawn the central tower
        BetterEndPodiumFeature endPodiumFeature = new BetterEndPodiumFeature(dragonFight.isFirstExitPortalSpawn(), isBottomOnly, isActive);
        BlockPos spawnPos = fightAccessor.getPortalLocation().below(5);
        endPodiumFeature.place(FeatureConfiguration.NONE, serverLevel, serverLevel.getChunkSource().getGenerator(), RandomSource.create(), spawnPos);
        dragonFight.setIsFirstExitPortalSpawn(false);
    }

    /**
     * Returns the exit portal location, adjusting it if necessary to ensure it is at a valid position
     * @param portalLocation The current portal location
     * @param serverLevel The server level
     * @return The adjusted portal location, ensuring it is at a valid position
     */
    private static BlockPos getAdjustedPortalPos(BlockPos portalLocation, ServerLevel serverLevel) {
        BlockPos portalPos = portalLocation;

        // Find the portal location if it hasn't been found yet
        if (portalPos == null || portalPos.getY() < 5) {
            // Log the current state of the portal location
            if (portalPos == null) {
                BetterEndIslandCommon.LOGGER.info("Portal location is null. Placing manually...");
            } else {
                BetterEndIslandCommon.LOGGER.info("Portal location is too low: {}. Placing manually...", portalPos.getY());
            }

            // Set the portal location to the surface at the center of the island
            portalPos = new BlockPos(0, WorldgenUtils.getLowestBlockPosAt(serverLevel, 0, 0), 0);
            while (serverLevel.getBlockState(portalPos).is(Blocks.BEDROCK) && portalPos.getY() > serverLevel.getSeaLevel()) {
                portalPos = portalPos.below();
            }

            // If the portal location is still too low, set it to a default value
            if (portalPos.getY() < 5) {
                BetterEndIslandCommon.LOGGER.info("Portal was still placed too low! Force placing at y=65...");
                portalPos = new BlockPos(0, 65, 0);
            }
        }

        BetterEndIslandCommon.LOGGER.info("Set the exit portal location to: {}", portalPos);
        return portalPos;
    }
}


