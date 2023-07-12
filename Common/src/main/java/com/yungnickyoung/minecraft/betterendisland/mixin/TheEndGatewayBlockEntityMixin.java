package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(TheEndGatewayBlockEntity.class)
public abstract class TheEndGatewayBlockEntityMixin {
    private static final TagKey<Block> CANNOT_PLACE_ON = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(BetterEndIslandCommon.MOD_ID, "end_gateway_cannot_place_player_on"));

    @Shadow
    private static Vec3 findExitPortalXZPosTentative(ServerLevel $$0, BlockPos $$1) {
        throw new AssertionError("Invalid mixin");
    }

    @Shadow
    private static LevelChunk getChunk(Level $$0, Vec3 $$1) {
        throw new AssertionError("Invalid mixin");
    }

    @Shadow
    private static BlockPos findTallestBlock(BlockGetter $$0, BlockPos $$1, int $$2, boolean $$3) {
        throw new AssertionError("Invalid mixin");
    }

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

    // Same as vanilla but offsets fresh island features so that it's centered with the gateway
//    @Inject(method = "findOrCreateValidTeleportPos", at = @At("HEAD"), cancellable = true)
//    private static void betterendisland_findOrCreateValidTeleportPos(ServerLevel $$0, BlockPos $$1, CallbackInfoReturnable<BlockPos> cir) {
//        Vec3 exitPortalPos = findExitPortalXZPosTentative($$0, $$1);
//        LevelChunk chunk = getChunk($$0, exitPortalPos);
//        BlockPos teleportPos = betterendisland_findValidSpawnInChunk(chunk);
//
//        if (teleportPos == null) {
//            teleportPos = new BlockPos(exitPortalPos.x + 0.5D, 75.0D, exitPortalPos.z + 0.5D);
//            BlockPos islandPos = teleportPos.offset(-3, 0, -3);
//            BetterEndIslandCommon.LOGGER.info("Failed to find a suitable block to teleport to, spawning an island on {}", islandPos);
//            EndFeatures.END_ISLAND.value().place($$0, $$0.getChunkSource().getGenerator(), RandomSource.create(islandPos.asLong()), islandPos);
//        } else {
//            BetterEndIslandCommon.LOGGER.info("Found suitable block to teleport to: {}", teleportPos);
//        }
//
//        cir.setReturnValue(findTallestBlock($$0, teleportPos, 16, true));
//    }

    // Same as vanilla, but improved check for valid standing position
    @Nullable
    @Unique
    private static BlockPos betterendisland_findValidSpawnInChunk(LevelChunk chunk) {
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

        return chosenPos;
    }
}
