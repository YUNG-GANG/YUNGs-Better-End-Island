package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndGatewayFeature;
import net.minecraft.world.level.levelgen.feature.EndGatewayFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndGatewayFeature.class)
public abstract class EndGatewayFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void betterendisland_placeEndGateway(FeaturePlaceContext<EndGatewayConfiguration> ctx, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(BetterEndGatewayFeature.place(ctx));
    }
}
