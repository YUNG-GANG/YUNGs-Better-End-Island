package com.yungnickyoung.minecraft.betterendisland.world.util;

import com.google.common.collect.Lists;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.IBetterDragonFight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EndCrystalUtils {
    private static final int BEI_CRYSTAL_RADIUS = 7;
    private static final int VANILLA_CRYSTAL_RADIUS = 2;

    /**
     * Checks for valid End Crystals in the new crystal respawn positions provided by BEI.
     */
    public static List<EndCrystal> checkForBEIRespawnCrystals(@Nullable Level level, BlockPos centerPos) {
        List<EndCrystal> foundCrystals = Lists.newArrayList();
        if (level == null) return foundCrystals;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            AABB crystalCheckbox = new AABB(centerPos.relative(direction, BEI_CRYSTAL_RADIUS));
            List<EndCrystal> crystalsInDirection = level.getEntitiesOfClass(EndCrystal.class, crystalCheckbox);
            foundCrystals.addAll(crystalsInDirection);
        }

        return foundCrystals;
    }

    /**
     * Checks for valid End Crystals in the vanilla respawn positions.
     */
    public static List<EndCrystal> checkForVanillaRespawnCrystals(@Nullable Level level, IBetterDragonFight dragonFight, BlockPos portalPos) {
        BlockPos centerPos = portalPos.below(2);

        // Adjust centerPos y-value based on settings
        if (!dragonFight.hasDragonEverSpawned() && !BetterEndIslandCommon.CONFIG.spawnCentralTowerInitially) {
            centerPos = centerPos.above(4);
        } else if (dragonFight.hasDragonEverSpawned()
                && !BetterEndIslandCommon.CONFIG.spawnCentralTowerInitially
                && !BetterEndIslandCommon.CONFIG.spawnCentralTowerOnResummon) {
            centerPos = centerPos.above(4);
        }

        List<EndCrystal> foundCrystals = Lists.newArrayList();
        if (level == null) return foundCrystals;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            AABB crystalCheckbox = new AABB(centerPos.relative(direction, VANILLA_CRYSTAL_RADIUS));
            List<EndCrystal> crystalsInDirection = level.getEntitiesOfClass(EndCrystal.class, crystalCheckbox);
            foundCrystals.addAll(crystalsInDirection);
        }

        return foundCrystals;
    }
}
