package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterSpikeFeature;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EndSpikeFeature.class)
public abstract class EndSpikeFeatureMixin {
    @Inject(method = "getSpikesForLevel", at = @At("HEAD"), cancellable = true)
    private static void betterendisland_getSpikesForLevel(WorldGenLevel level, CallbackInfoReturnable<List<EndSpikeFeature.EndSpike>> cir) {
        cir.setReturnValue(BetterSpikeFeature.getSpikesForLevel(level));
    }

    @Inject(method = "placeSpike", at = @At("HEAD"), cancellable = true)
    private void betterendisland_placeSpike(ServerLevelAccessor level, RandomSource random, EndSpikeFeature.EndSpike spike, CallbackInfo ci) {
        BetterSpikeFeature.placeSpike(level, random, (EndSpikeFeature) (Object) this, spike, level instanceof WorldGenRegion);
        ci.cancel();
    }
}
