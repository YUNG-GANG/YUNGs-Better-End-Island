package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;


/**
 * Mixins for fixing compatibility issues with Endergetic Expansion.
 */
@Mixin(value = ServerLevel.class, priority = 2000)
public abstract class EndergeticExpansionMixins extends Level {
    @Shadow
    @Nullable
    private EnderDragonFight dragonFight;

    @Shadow
    public abstract SavedDataStorage getDataStorage();

    protected EndergeticExpansionMixins(WritableLevelData $$0, ResourceKey<Level> $$1, RegistryAccess $$2, Holder<DimensionType> $$3, boolean $$4, boolean $$5, long $$6, int $$7) {
        super($$0, $$1, $$2, $$3, $$4, $$5, $$6, $$7);
    }

    /**
     * Mixin to overwrite Endergetic Expansion's EndergeticDragonFightManager with the vanilla one, which we inject into.
     */
    @Inject(at = @At("RETURN"), method = "<init>")
    private void betterendisland_overwriteModdedDragonFight(final MinecraftServer server,
                                                            final Executor executor,
                                                            final LevelStorageSource.LevelStorageAccess levelStorage,
                                                            final ServerLevelData levelData,
                                                            final ResourceKey<Level> dimension,
                                                            final LevelStem levelStem,
                                                            final boolean isDebug,
                                                            final long biomeZoomSeed,
                                                            final List<CustomSpawner> customSpawners,
                                                            final boolean tickTime,
                                                            CallbackInfo ci) {
        if (BetterEndIslandCommon.endergetic && this.dragonFight != null) {
            WorldGenSettings worldGenSettings = server.getWorldGenSettings();
            WorldOptions options = worldGenSettings.options();
            long seed = options.seed();
            this.dragonFight = this.getDataStorage().computeIfAbsent(EnderDragonFight.TYPE);
            this.dragonFight.init((ServerLevel) (Level) this, seed, BlockPos.ZERO);
        }
    }
}
