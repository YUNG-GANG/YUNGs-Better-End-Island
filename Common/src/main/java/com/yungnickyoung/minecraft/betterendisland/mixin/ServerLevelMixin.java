package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.world.ExtraFightData;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.IPrimaryLevelData;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    protected ServerLevelMixin(WritableLevelData $$0, ResourceKey<Level> $$1, RegistryAccess $$2, Holder<DimensionType> $$3, Supplier<ProfilerFiller> $$4, boolean $$5, boolean $$6, long $$7, int $$8) {
        super($$0, $$1, $$2, $$3, $$4, $$5, $$6, $$7, $$8);
    }

    @Shadow @Nullable public abstract EndDragonFight getDragonFight();

    @Shadow @Nullable private EndDragonFight dragonFight;
    @Shadow @Final private MinecraftServer server;
    @Unique ResourceLocation END_DIMENSION = ResourceLocation.withDefaultNamespace("the_end");

    @Inject(method = "<init>", at = @At("RETURN"))
    private void betterendisland_attachExtraData1(MinecraftServer server, Executor $$1, LevelStorageSource.LevelStorageAccess $$2, ServerLevelData $$3, ResourceKey $$4, LevelStem $$5, ChunkProgressListener $$6, boolean $$7, long $$8, List $$9, boolean $$10, RandomSequences $$11, CallbackInfo ci) {
        if (this.dragonFight != null) {
            this.dragonFight = new EndDragonFight((ServerLevel) (Object) this,
                    server.getWorldData().worldGenOptions().seed(),
                    server.getWorldData().endDragonFightData());
            ExtraFightData extraFightData = ((IPrimaryLevelData) (server.getWorldData())).getExtraEndDragonFightData();
            ((IDragonFight) dragonFight).betterendisland$setFirstExitPortalSpawn(extraFightData.firstExitPortalSpawn());
            ((IDragonFight) dragonFight).betterendisland$setHasDragonEverSpawned(extraFightData.hasDragonEverSpawned());
            ((IDragonFight) dragonFight).betterendisland$setNumTimesDragonKilled(extraFightData.numTimesDragonKilled());
        }
    }

    @Inject(method = "saveLevelData", at = @At("HEAD"))
    private void betterendisland_attachExtraData2(CallbackInfo ci) {
        if (this.dragonFight != null) {
            ExtraFightData extraFightData = new ExtraFightData(
                    ((IDragonFight) this.dragonFight).betterendisland$firstExitPortalSpawn(),
                    ((IDragonFight) this.dragonFight).betterendisland$hasDragonEverSpawned(),
                    ((IDragonFight) this.dragonFight).betterendisland$numTimesDragonKilled()
            );
            ((IPrimaryLevelData) this.server.getWorldData()).setExtraEndDragonFightData(extraFightData);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void betterendisland_tickInitialDragonSummonTrigger(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        betterendisland$tickSummonDragonFight();
        betterendisland$tickBellSound();
    }

    @Unique
    private void betterendisland$tickBellSound() {
        if (this.dimension().location().equals(END_DIMENSION) && this.getDragonFight() != null) {
            ((IDragonFight) this.getDragonFight()).betterendisland$tickBellSound();
        }
    }

    @Unique
    private void betterendisland$tickSummonDragonFight() {
        if (this.dimension().location().equals(END_DIMENSION)
                && this.getDragonFight() != null
                && !((IDragonFight) this.getDragonFight()).betterendisland$hasDragonEverSpawned()
                && ((IDragonFight) this.getDragonFight()).betterendisland$getDragonRespawnStage() == null
                && this.levelData.getGameTime() % 5 == 0)
        {
            double minDistance = -1.0D;
            double requiredDistance = 25.0D;
            Player foundPlayer = null;

            for (Player player : this.players()) {
                if (EntitySelector.NO_SPECTATORS.test(player)) {
                    double distance = xzDistanceSqr(0, 0, player.position().x(), player.position().z()); // (0, 0) is the center of the island
                    if (distance < requiredDistance * requiredDistance && (minDistance == -1.0D || distance < minDistance)) {
                        minDistance = distance;
                        foundPlayer = player;
                    }
                }
            }

            // Non-null foundPlayer means we found a player in range to trigger summoning the dragon
            if (foundPlayer != null) {
                ((IDragonFight) this.getDragonFight()).betterendisland$initialRespawn();
            }
        }
    }

    @Unique
    private double xzDistanceSqr(double x1, double z1, double x2, double z2) {
        double xDist = x2 - x1;
        double zDist = z2 - z1;
        return xDist * xDist + zDist * zDist;
    }
}
