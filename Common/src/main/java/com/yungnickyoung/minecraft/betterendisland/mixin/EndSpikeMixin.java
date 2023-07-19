package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.world.IEndSpike;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpikeFeature.EndSpike.class)
public abstract class EndSpikeMixin implements IEndSpike {
    @Shadow @Final @Mutable private AABB topBoundingBox;
    @Shadow @Final private int height;

    @Unique private int betterendisland$crystalHeight = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void betterendisland_adjustSpikeBoundingBox(int centerX, int centerZ, int radius, int height, boolean guarded, CallbackInfo ci) {
        this.topBoundingBox = new AABB(centerX - 9, DimensionType.MIN_Y, centerZ - 9, centerX + 9, DimensionType.MAX_Y, centerZ + 9);
    }

    /**
     * This is a hack to override Better End's spike height calculation.
     */
    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void betterendisland_getSpikeHeight(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.height);
    }

    @Override
    public int betterendisland$getCrystalYOffset() {
        return betterendisland$crystalHeight;
    }

    @Override
    public void betterendisland$setCrystalYOffsetFromPillarHeight(int pillarHeight) {
        this.betterendisland$crystalHeight = switch (pillarHeight) {
            case 8,7,6 -> 32;
            case 5,4 -> 27;
            case 3 -> 26;
            case 2,1 -> 22;
            default -> 38; // case 9
        };
    }
}
