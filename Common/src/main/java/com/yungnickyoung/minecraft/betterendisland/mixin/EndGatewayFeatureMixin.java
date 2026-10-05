package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndGatewayFeature;
import net.minecraft.world.level.levelgen.feature.EndGatewayFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndGatewayFeature.class)
public abstract class EndGatewayFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void betterendisland_placeEndGateway(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random,
                                                  BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        if (BetterEndIslandCommon.CONFIG.useVanillaEndGateways) return;
        cir.setReturnValue(BetterEndGatewayFeature.place((EndGatewayFeature) (Object) this, level, random, origin));
    }
}
