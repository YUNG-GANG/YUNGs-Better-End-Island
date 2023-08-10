package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TheEndGatewayBlockEntity.class)
public abstract class TheEndGatewayBlockEntityMixin {
    @Unique private static final TagKey<Block> CANNOT_PLACE_ON = TagKey.create(Registries.BLOCK, new ResourceLocation(BetterEndIslandCommon.MOD_ID, "end_gateway_cannot_place_player_on"));

    @Inject(method = "findTallestBlock", at = @At("HEAD"), cancellable = true)
    private static void betterendisland_findTallestBlock(BlockGetter level, BlockPos pos, int radius, boolean placeAnywhere, CallbackInfoReturnable<BlockPos> cir) {
        BlockPos targetPos = null;

        for (int xOffset = -radius; xOffset <= radius; ++xOffset) {
            for (int zOffset = -radius; zOffset <= radius; ++zOffset) {
                if (xOffset != 0 || zOffset != 0 || placeAnywhere) {
                    for (int y = level.getMaxBuildHeight() - 1; y > (targetPos == null ? level.getMinBuildHeight() : targetPos.getY()); --y) {
                        BlockPos candidatePos = new BlockPos(pos.getX() + xOffset, y, pos.getZ() + zOffset);
                        BlockState blockState = level.getBlockState(candidatePos);
                        if (blockState.isCollisionShapeFullBlock(level, candidatePos) && (placeAnywhere || !blockState.is(CANNOT_PLACE_ON))) {
                            targetPos = candidatePos;
                            break;
                        }
                    }
                }
            }
        }

        cir.setReturnValue(targetPos == null ? pos : targetPos);
    }

    // Same as vanilla, but improved check for valid standing position
    @Inject(method = "findValidSpawnInChunk", at = @At("HEAD"), cancellable = true)
    private static void betterendisland_findValidSpawnInChunk(LevelChunk chunk, CallbackInfoReturnable<BlockPos> cir) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos minPos = new BlockPos(chunkPos.getMinBlockX(), 30, chunkPos.getMinBlockZ());
        int maxY = chunk.getHighestSectionPosition() + 16 - 1;
        BlockPos maxPos = new BlockPos(chunkPos.getMaxBlockX(), maxY, chunkPos.getMaxBlockZ());
        BlockPos chosenPos = null;
        double minDistance = 0.0D;

        for(BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState blockState = chunk.getBlockState(pos);
            BlockPos above = pos.above();
            BlockPos above2 = pos.above(2);
            if (blockState.is(Blocks.END_STONE) && chunk.getBlockState(above).isAir() && chunk.getBlockState(above2).isAir()) {
                double distance = pos.distToCenterSqr(0.0D, 0.0D, 0.0D);
                if (chosenPos == null || distance < minDistance) {
                    chosenPos = pos;
                    minDistance = distance;
                }
            }
        }

        cir.setReturnValue(chosenPos);
    }
}
