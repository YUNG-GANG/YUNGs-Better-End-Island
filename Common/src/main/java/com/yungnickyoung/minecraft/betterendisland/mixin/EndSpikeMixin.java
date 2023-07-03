package com.yungnickyoung.minecraft.betterendisland.mixin;

import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpikeFeature.EndSpike.class)
public abstract class EndSpikeMixin {
    @Shadow @Final @Mutable
    private AABB topBoundingBox;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void betterendisland_adjustSpikeBoundingBox(int centerX, int centerZ, int radius, int height, boolean guarded, CallbackInfo ci) {
        this.topBoundingBox = new AABB(centerX - 9, DimensionType.MIN_Y, centerZ - 9, centerX + 9, DimensionType.MAX_Y, centerZ + 9);
    }
}
