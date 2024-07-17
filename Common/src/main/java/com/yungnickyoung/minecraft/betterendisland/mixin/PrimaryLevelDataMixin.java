package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.ExtraFightData;
import com.yungnickyoung.minecraft.betterendisland.world.IPrimaryLevelData;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin implements IPrimaryLevelData {
    @Unique private ExtraFightData betterendisland$endDragonFightData = ExtraFightData.DEFAULT;

    @Inject(method = "parse", at = @At("RETURN"))
    private static <T> void betterendisland_attachExtraFightData1(Dynamic<T> dynamic, LevelSettings $$1, PrimaryLevelData.SpecialWorldProperty $$2, WorldOptions $$3, Lifecycle $$4, CallbackInfoReturnable<PrimaryLevelData> cir) {
        PrimaryLevelData data = cir.getReturnValue();
        ExtraFightData extraFightData = dynamic.get("bei_ExtraDragonFight")
                .read(ExtraFightData.CODEC)
                .resultOrPartial(BetterEndIslandCommon.LOGGER::error)
                .orElse(ExtraFightData.DEFAULT);
        ((IPrimaryLevelData) data).setExtraEndDragonFightData(extraFightData);
    }

    @Inject(method = "setTagData", at = @At("RETURN"))
    private void betterendisland_attachExtraFightData2(RegistryAccess registryAccess, CompoundTag tag, CompoundTag $$2, CallbackInfo ci) {
        ExtraFightData.CODEC.encodeStart(NbtOps.INSTANCE, this.betterendisland$endDragonFightData)
                .resultOrPartial(e -> BetterEndIslandCommon.LOGGER.error("Failed to encode bei_ExtraDragonFight: {}", e))
                .ifPresent(extraFightDataTag -> tag.put("bei_ExtraDragonFight", extraFightDataTag));
    }

    @Unique
    @Override
    public void setExtraEndDragonFightData(ExtraFightData extraFightData) {
        this.betterendisland$endDragonFightData = extraFightData;
    }

    @Override
    public ExtraFightData getExtraEndDragonFightData() {
        return this.betterendisland$endDragonFightData;
    }
}
