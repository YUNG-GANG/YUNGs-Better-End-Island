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

@Mixin(SpikeFeature.EndSpike.class)
public abstract class EndSpikeMixin implements IEndSpike {
    @Shadow @Final @Mutable
    private AABB topBoundingBox;

    @Unique
    private int crystalHeight = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void betterendisland_adjustSpikeBoundingBox(int centerX, int centerZ, int radius, int height, boolean guarded, CallbackInfo ci) {
        this.topBoundingBox = new AABB(centerX - 9, DimensionType.MIN_Y, centerZ - 9, centerX + 9, DimensionType.MAX_Y, centerZ + 9);
    }

    @Override
    public int getCrystalYOffset() {
        return crystalHeight;
    }

    @Override
    public void setCrystalYOffsetFromPillarHeight(int pillarHeight) {
        this.crystalHeight = switch (pillarHeight) {
            case 8,7,6 -> 32;
            case 5,4 -> 27;
            case 3 -> 26;
            case 2,1 -> 22;
            default -> 38; // case 9
        };
    }
}
