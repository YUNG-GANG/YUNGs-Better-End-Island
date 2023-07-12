package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndSpawnPlatformFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "createEndPlatform", at = @At("HEAD"), cancellable = true)
    private void betterendisland_createEndPlatform(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        BetterEndSpawnPlatformFeature.place(level, pos);
        ci.cancel();
    }
}
