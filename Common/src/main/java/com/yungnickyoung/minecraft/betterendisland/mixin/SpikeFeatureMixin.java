package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterSpikeFeature;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SpikeFeature.class)
public abstract class SpikeFeatureMixin {
    @Inject(method = "getSpikesForLevel", at = @At("HEAD"), cancellable = true)
    private static void betterendisland_getSpikesForLevel(WorldGenLevel level, CallbackInfoReturnable<List<SpikeFeature.EndSpike>> cir) {
        cir.setReturnValue(BetterSpikeFeature.getSpikesForLevel(level));
    }

    @Inject(method = "placeSpike", at = @At("HEAD"), cancellable = true)
    private void betterendisland_placeSpike(ServerLevelAccessor level, RandomSource randomSource, SpikeConfiguration config, SpikeFeature.EndSpike spike, CallbackInfo ci) {
        BetterSpikeFeature.placeSpike(level, randomSource, config, spike, level instanceof WorldGenRegion);
        ci.cancel();
    }
}
