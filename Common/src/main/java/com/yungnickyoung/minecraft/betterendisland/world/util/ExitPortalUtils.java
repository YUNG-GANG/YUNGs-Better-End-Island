package com.yungnickyoung.minecraft.betterendisland.world.util;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.mixin.accessor.EnderDragonFightAccessor;
import com.yungnickyoung.minecraft.betterendisland.world.IBetterDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndPodiumFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class ExitPortalUtils {
    /**
     * Spawns the exit portal at the specified location, ensuring it is at a valid position
     * @param dragonFight The dragon fight instance
     * @param serverLevel The server level
     * @param isActive Whether the portal is active
     * @param isBottomOnly Whether only the bottom half of the portal should be spawned.
     *                     This is used in non-first exit portal spawns to prevent the top half from being replaced.
     * @param noCrystalsOverride Override flag that, when set, prevents crystals from being placed, no matter the dragon fight state.
     */
    public static void spawnPortal(IBetterDragonFight dragonFight, ServerLevel serverLevel,
                                   boolean isActive, boolean isBottomOnly, boolean noCrystalsOverride) {
        boolean useBetterPortal = dragonFight.hasDragonEverSpawned()
                ? BetterEndIslandCommon.CONFIG.spawnCentralTowerOnResummon
                : BetterEndIslandCommon.CONFIG.spawnCentralTowerInitially;

        EnderDragonFightAccessor fightAccessor = (EnderDragonFightAccessor) dragonFight;

        // Update the portal location to ensure it is set to the correct position
        fightAccessor.setPortalLocation(getAdjustedPortalPos(fightAccessor.getPortalLocation(), serverLevel));

        if (useBetterPortal) {
            // Spawn the central tower
            BetterEndPodiumFeature endPodiumFeature = new BetterEndPodiumFeature(dragonFight.isFirstExitPortalSpawn(), isBottomOnly, isActive);
            BlockPos spawnPos = fightAccessor.getPortalLocation().below(5);
            endPodiumFeature.place(FeatureConfiguration.NONE, serverLevel, serverLevel.getChunkSource().getGenerator(), RandomSource.create(), spawnPos);
        } else {
            // Spawn the vanilla podium
            EndPodiumFeature endPodiumFeature = new EndPodiumFeature(isActive);
            BlockPos spawnPos = fightAccessor.getPortalLocation().below(3);

            // Adjust spawn position so that the portal isn't buried in the ground
            if (!dragonFight.hasDragonEverSpawned() || !BetterEndIslandCommon.CONFIG.spawnCentralTowerInitially) {
                spawnPos = spawnPos.above(4);
            }

            if (endPodiumFeature.place(FeatureConfiguration.NONE, serverLevel, serverLevel.getChunkSource().getGenerator(), RandomSource.create(), spawnPos)) {
                int $$2 = Mth.positiveCeilDiv(4, 16);
                serverLevel.getChunkSource().chunkMap.waitForLightBeforeSending(ChunkPos.containing(spawnPos), $$2);
            }

            // Place crystals on initial spawn
            if (!dragonFight.hasDragonEverSpawned() && !noCrystalsOverride) {
                BlockPos centerPos = spawnPos.above(1);
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos crystalPos = centerPos.relative(direction, 3);
                    EndCrystal crystal = new EndCrystal(serverLevel, crystalPos.getX() + 0.5D, crystalPos.getY(), crystalPos.getZ() + 0.5D);
                    crystal.setShowBottom(false);
                    crystal.setInvulnerable(true); // Prevent player destroying crystals, which would result in a soft lock
                    serverLevel.addFreshEntity(crystal);
                }
            }
        }

        dragonFight.setIsFirstExitPortalSpawn(false);
    }

    public static void spawnPortal(IBetterDragonFight dragonFight, ServerLevel serverLevel,
                                   boolean isActive, boolean isBottomOnly) {
        spawnPortal(dragonFight, serverLevel, isActive, isBottomOnly, false);
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
            portalPos = new BlockPos(0, WorldgenUtils.getSurfacePosAt(serverLevel, 0, 0), 0);
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


