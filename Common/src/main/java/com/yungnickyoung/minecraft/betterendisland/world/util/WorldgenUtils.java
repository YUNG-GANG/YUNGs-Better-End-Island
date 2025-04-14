package com.yungnickyoung.minecraft.betterendisland.world.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class WorldgenUtils {
    /**
     * Returns the y-value of the lowest block at the given x and z coordinates.
     */
    public static int getLowestBlockPosAt(Level level, int x, int z) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
            mutable.set(x, y, z);
            if (level.getBlockState(mutable).is(Blocks.END_STONE)) {
                return y;
            }
        }
        return 255;
    }

    /**
     * Returns the y-value of the highest block at the given x and z coordinates.
     */
    public static int getSurfacePosAt(Level level, int x, int z) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = level.getMaxY(); y > level.getMinY(); y--) {
            mutable.set(x, y, z);
            if (level.getBlockState(mutable).is(Blocks.END_STONE)) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Returns the square of the distance between two 2-D points.
     */
    public static double distSqr(double x1, double z1, double x2, double z2) {
        double xDist = x2 - x1;
        double zDist = z2 - z1;
        return xDist * xDist + zDist * zDist;
    }
}
