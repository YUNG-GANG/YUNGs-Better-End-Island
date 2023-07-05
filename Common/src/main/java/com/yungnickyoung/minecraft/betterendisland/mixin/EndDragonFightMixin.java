package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.google.common.collect.Lists;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.DragonRespawnStage;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Mixin(EndDragonFight.class)
public abstract class EndDragonFightMixin implements IDragonFight {
    @Shadow @Final private ServerBossEvent dragonEvent;
    @Shadow private boolean dragonKilled;
    @Shadow private int ticksSinceLastPlayerScan;

    @Shadow protected abstract void updatePlayers();

    @Shadow @Final private ServerLevel level;

    @Shadow protected abstract boolean isArenaLoaded();

    @Shadow private boolean needsStateScanning;

    @Shadow protected abstract void scanState();

    @Shadow @Nullable private List<EndCrystal> respawnCrystals;

    @Shadow public abstract void tryRespawn();

    @Shadow private int respawnTime;
    @Shadow @Nullable private BlockPos portalLocation;
    @Shadow @Nullable private UUID dragonUUID;
    @Shadow private int ticksSinceDragonSeen;

    @Shadow protected abstract void findOrCreateDragon();

    @Shadow private int ticksSinceCrystalsScanned;

    @Shadow protected abstract void updateCrystalCount();

    @Shadow protected abstract EnderDragon createNewDragon();

    @Shadow public abstract void resetSpikeCrystals();

    @Shadow protected abstract void spawnExitPortal(boolean $$0);

    @Shadow @Nullable protected abstract BlockPattern.BlockPatternMatch findExitPortal();

    @Shadow protected abstract void respawnDragon(List<EndCrystal> $$0);

    @Shadow @Final private BlockPattern exitPortalPattern;
    @Unique
    private DragonRespawnStage dragonRespawnStage;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void betterendisland_EndDragonFight(ServerLevel level, long seed, CompoundTag tag, CallbackInfo ci) {
        if (tag.getBoolean("IsRespawning")) {
            this.dragonRespawnStage = DragonRespawnStage.START;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void betterendisland_tickFight(CallbackInfo ci) {
        this.dragonEvent.setVisible(!this.dragonKilled);
        if (++this.ticksSinceLastPlayerScan >= 20) {
            this.updatePlayers();
            this.ticksSinceLastPlayerScan = 0;
        }

        if (!this.dragonEvent.getPlayers().isEmpty()) {
            this.level.getChunkSource().addRegionTicket(TicketType.DRAGON, new ChunkPos(0, 0), 9, Unit.INSTANCE);
            boolean isArenaLoaded = this.isArenaLoaded();
            if (this.needsStateScanning && isArenaLoaded) {
                this.scanState();
                this.needsStateScanning = false;
            }

            if (this.dragonRespawnStage != null) {
                if (this.respawnCrystals == null && isArenaLoaded) {
                    this.dragonRespawnStage = null;
                    this.tryRespawn();
                }

                this.dragonRespawnStage.tick(this.level, (EndDragonFight) (Object) this, this.respawnCrystals, this.respawnTime++, this.portalLocation);
            }

            if (!this.dragonKilled) {
                if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && isArenaLoaded) {
                    this.findOrCreateDragon();
                    this.ticksSinceDragonSeen = 0;
                }

                if (++this.ticksSinceCrystalsScanned >= 100 && isArenaLoaded) {
                    this.updateCrystalCount();
                    this.ticksSinceCrystalsScanned = 0;
                }
            }
        } else {
            this.level.getChunkSource().removeRegionTicket(TicketType.DRAGON, new ChunkPos(0, 0), 9, Unit.INSTANCE);
        }
        ci.cancel();
    }

    @Inject(method = "onCrystalDestroyed", at = @At("HEAD"), cancellable = true)
    public void betterendisland_onCrystalDestroyed(EndCrystal crystal, DamageSource damageSource, CallbackInfo ci) {
        if (this.dragonRespawnStage != null && this.respawnCrystals.contains(crystal)) {
            BetterEndIslandCommon.LOGGER.debug("Aborting dragon respawn sequence");
            this.dragonRespawnStage = null;
            this.respawnTime = 0;
            this.resetSpikeCrystals();
            this.spawnExitPortal(true);
        } else {
            this.updateCrystalCount();
            Entity dragonEntity = this.level.getEntity(this.dragonUUID);
            if (dragonEntity instanceof EnderDragon) {
                ((EnderDragon)dragonEntity).onCrystalDestroyed(crystal, crystal.blockPosition(), damageSource);
            }
        }
        ci.cancel();
    }

    @Inject(method = "tryRespawn", at = @At("HEAD"), cancellable = true)
    public void betterendisland_tryRespawn(CallbackInfo ci) {
        if (this.dragonKilled && this.dragonRespawnStage == null) {
            BlockPos portalPos = this.portalLocation;
            if (portalPos == null) {
                BetterEndIslandCommon.LOGGER.debug("Tried to respawn, but need to find the portal first.");
                BlockPattern.BlockPatternMatch portalPatternMatch = this.findExitPortal();
                if (portalPatternMatch == null) {
                    BetterEndIslandCommon.LOGGER.debug("Couldn't find a portal, so we made one.");
                    this.spawnExitPortal(true);
                } else {
                    BetterEndIslandCommon.LOGGER.debug("Found the exit portal & saved its location for next time.");
                }

                portalPos = this.portalLocation;
            }

            List<EndCrystal> allCrystals = Lists.newArrayList();
            BlockPos $$3 = portalPos.above(1);

            for(Direction direction : Direction.Plane.HORIZONTAL) {
                List<EndCrystal> crystalsInDirection = this.level.getEntitiesOfClass(EndCrystal.class, new AABB($$3.relative(direction, 2)));
                if (crystalsInDirection.isEmpty()) {
                    return;
                }

                allCrystals.addAll(crystalsInDirection);
            }

            BetterEndIslandCommon.LOGGER.debug("Found all crystals, respawning dragon.");
            this.respawnDragon(allCrystals);
        }
        ci.cancel();
    }

    @Inject(method = "respawnDragon", at = @At("HEAD"), cancellable = true)
    private void betterendisland_respawnDragon(List<EndCrystal> crystals, CallbackInfo ci) {
        if (this.dragonKilled && this.dragonRespawnStage == null) {
            for(BlockPattern.BlockPatternMatch portalPatternMatch = this.findExitPortal(); portalPatternMatch != null; portalPatternMatch = this.findExitPortal()) {
                for(int x = 0; x < this.exitPortalPattern.getWidth(); ++x) {
                    for(int y = 0; y < this.exitPortalPattern.getHeight(); ++y) {
                        for(int z = 0; z < this.exitPortalPattern.getDepth(); ++z) {
                            BlockInWorld patternBlock = portalPatternMatch.getBlock(x, y, z);
                            if (patternBlock.getState().is(Blocks.BEDROCK) || patternBlock.getState().is(Blocks.END_PORTAL)) {
                                this.level.setBlockAndUpdate(patternBlock.getPos(), Blocks.END_STONE.defaultBlockState());
                            }
                        }
                    }
                }
            }

            this.dragonRespawnStage = DragonRespawnStage.START;
            this.respawnTime = 0;
            this.spawnExitPortal(false);
            this.respawnCrystals = crystals;
        }
        ci.cancel();
    }

    @Override
    public void setDragonRespawnStage(DragonRespawnStage stage) {
        if (this.dragonRespawnStage == null) {
            throw new IllegalStateException("Better Dragon respawn isn't in progress, can't skip ahead in the animation.");
        } else {
            this.respawnTime = 0;
            if (stage == DragonRespawnStage.END) {
                this.dragonRespawnStage = null;
                this.dragonKilled = false;
                EnderDragon newDragon = this.createNewDragon();
                for (ServerPlayer serverPlayer : this.dragonEvent.getPlayers()) {
                    CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, newDragon);
                }
            } else {
                this.dragonRespawnStage = stage;
            }
        }
    }

    @Override
    public DragonRespawnStage getDragonRespawnStage() {
        return null;
    }
}
