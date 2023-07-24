package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.services.Services;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;


/**
 * Mixins for fixing compatibility issues with Endergetic Expansion.
 */
@Mixin(value = ServerLevel.class, priority = 2000)
public abstract class EndergeticExpansionMixins extends Level {
    @Shadow @Final @Mutable @Nullable private EndDragonFight dragonFight;

    protected EndergeticExpansionMixins(WritableLevelData $$0, ResourceKey<Level> $$1, Holder<DimensionType> $$2, Supplier<ProfilerFiller> $$3, boolean $$4, boolean $$5, long $$6, int $$7) {
        super($$0, $$1, $$2, $$3, $$4, $$5, $$6, $$7);
    }

    /**
     * Mixin to overwrite Endergetic Expansion's EndergeticDragonFightManager with the vanilla one, which we inject into.
     */
    @Inject(at = @At("RETURN"), method = "<init>")
    private void betterendisland_overwriteModdedDragonFight(MinecraftServer server, Executor p_i232604_2_, LevelStorageSource.LevelStorageAccess p_i232604_3_, ServerLevelData p_i232604_4_, ResourceKey<Level> p_i232604_5_, LevelStem p_i232604_7_, ChunkProgressListener p_i232604_8_, boolean p_215006_, long p_215007_, List<CustomSpawner> p_215008_, boolean p_215009_, CallbackInfo info) {
        if (Services.PLATFORM.isModLoaded("endergetic") && this.dragonFight != null) {
            this.dragonFight = new EndDragonFight((ServerLevel) (Object) this, server.getWorldData().worldGenSettings().seed(), server.getWorldData().endDragonFightData());
        }
    }
}
