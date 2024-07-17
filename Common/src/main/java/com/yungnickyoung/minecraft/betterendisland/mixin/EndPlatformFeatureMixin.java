package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndSpawnPlatformFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPlatformFeature.class)
public abstract class EndPlatformFeatureMixin {
    @Inject(method = "createEndPlatform", at = @At("HEAD"), cancellable = true)
    private static void createEndPlatform(ServerLevelAccessor levelAccessor, BlockPos pos, boolean $$2, CallbackInfo ci) {
        if (!BetterEndIslandCommon.CONFIG.useVanillaSpawnPlatform) {
            BetterEndSpawnPlatformFeature.place(levelAccessor.getLevel(), pos);
            ci.cancel();
        }
    }
}
