package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.google.common.collect.Lists;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.DragonRespawnStage;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndPodiumFeature;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Mixin(EndDragonFight.class)
public abstract class EndDragonFightMixin implements IDragonFight {
    @Shadow public abstract void resetSpikeCrystals();
    @Shadow public abstract void tryRespawn();
    @Shadow protected abstract void updatePlayers();
    @Shadow protected abstract boolean isArenaLoaded();
    @Shadow protected abstract void findOrCreateDragon();
    @Shadow protected abstract void updateCrystalCount();
    @Shadow protected abstract EnderDragon createNewDragon();
    @Shadow @Nullable protected abstract BlockPattern.BlockPatternMatch findExitPortal();
    @Shadow protected abstract void respawnDragon(List<EndCrystal> $$0);
    @Shadow protected abstract boolean hasActiveExitPortal();
    @Shadow protected abstract void spawnNewGateway();

    @Shadow @Final private ServerBossEvent dragonEvent;
    @Shadow private boolean dragonKilled;
    @Shadow private int ticksSinceLastPlayerScan;
    @Shadow @Final private ServerLevel level;
    @Shadow private boolean needsStateScanning;
    @Shadow @Nullable private List<EndCrystal> respawnCrystals;
    @Shadow private int respawnTime;
    @Shadow @Nullable private BlockPos portalLocation;
    @Shadow @Nullable private UUID dragonUUID;
    @Shadow private int ticksSinceDragonSeen;
    @Shadow private int ticksSinceCrystalsScanned;
    @Shadow private boolean previouslyKilled;

    @Unique private DragonRespawnStage dragonRespawnStage;
    @Unique private boolean firstExitPortalSpawn = true;
    @Unique private boolean hasDragonEverSpawned;
    @Unique private int numberTimesDragonKilled = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void betterendisland_EndDragonFight(ServerLevel level, long seed, CompoundTag tag, CallbackInfo ci) {
        if (tag.getBoolean("IsRespawning")) {
            this.dragonRespawnStage = DragonRespawnStage.START;
        }
        if (tag.contains("FirstExitPortalSpawn")) {
            this.firstExitPortalSpawn = tag.getBoolean("FirstExitPortalSpawn");
        } else {
            this.firstExitPortalSpawn = true;
        }
        if (tag.contains("HasDragonEverSpawned")) {
            this.hasDragonEverSpawned = tag.getBoolean("HasDragonEverSpawned");
        } else {
            this.hasDragonEverSpawned = false;
        }
        if (tag.contains("NumberTimesDragonKilled")) {
            this.numberTimesDragonKilled = tag.getInt("NumberTimesDragonKilled");
        } else {
            this.numberTimesDragonKilled = 0;
        }
        this.dragonEvent.setVisible(false);
    }

    @Inject(method = "saveData", at = @At("RETURN"))
    public void betterendisland_saveData(CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putBoolean("FirstExitPortalSpawn", this.firstExitPortalSpawn);
        cir.getReturnValue().putBoolean("HasDragonEverSpawned", this.hasDragonEverSpawned);
        cir.getReturnValue().putInt("NumberTimesDragonKilled", this.numberTimesDragonKilled);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void betterendisland_tickFight(CallbackInfo ci) {
        this.dragonEvent.setVisible(!this.dragonKilled && this.hasDragonEverSpawned);
        if (++this.ticksSinceLastPlayerScan >= 20) {
            this.updatePlayers();
            this.ticksSinceLastPlayerScan = 0;
        }

        if (!this.dragonEvent.getPlayers().isEmpty()) {
            this.level.getChunkSource().addRegionTicket(TicketType.DRAGON, new ChunkPos(0, 0), 9, Unit.INSTANCE);
            boolean isArenaLoaded = this.isArenaLoaded();

            // Initial state scanning.
            // Only performed once, when the dimension is first loaded.
            if (this.needsStateScanning && isArenaLoaded) {
                this.scanForInitialState();
                this.needsStateScanning = false;
            }

            // Update respawn stage if performing respawn
            if (this.dragonRespawnStage != null) {
                if (this.respawnCrystals == null && isArenaLoaded) {
                    this.dragonRespawnStage = null;
                    this.tryRespawn();
                }

                this.dragonRespawnStage.tick(this.level, (EndDragonFight) (Object) this, this.respawnCrystals, this.respawnTime++, this.portalLocation);
            }

            if (!this.dragonKilled) {
                if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && isArenaLoaded && this.hasDragonEverSpawned) {
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

    @Unique
    private void scanForInitialState() {
        BetterEndIslandCommon.LOGGER.info("Scanning for legacy world dragon fight...");
        boolean hasActiveExitPortal = this.hasActiveExitPortal();
        if (hasActiveExitPortal) {
            BetterEndIslandCommon.LOGGER.info("Found that the dragon has been killed in this world already.");
            this.previouslyKilled = true;
        } else {
            BetterEndIslandCommon.LOGGER.info("Found that the dragon has not yet been killed in this world.");
            this.previouslyKilled = false;
            if (this.findExitPortal() == null) {
                this.spawnPortal(false, false);
            }
        }

        List<? extends EnderDragon> dragons = this.level.getDragons();
        if (dragons.isEmpty()) {
            this.dragonKilled = true;
        } else {
            EnderDragon dragon = dragons.get(0);
            this.dragonUUID = dragon.getUUID();
            BetterEndIslandCommon.LOGGER.info("Found that there's a dragon still alive ({})", dragon);
            this.dragonKilled = false;
            if (!hasActiveExitPortal) {
                BetterEndIslandCommon.LOGGER.info("But we didn't have a portal, so let's remove the dragon.");
                dragon.discard();
                this.dragonUUID = null;
            }
        }

        if (!this.previouslyKilled && this.dragonKilled) {
            this.dragonKilled = false;
        }
    }

    @Inject(method = "onCrystalDestroyed", at = @At("HEAD"), cancellable = true)
    public void betterendisland_onCrystalDestroyed(EndCrystal crystal, DamageSource damageSource, CallbackInfo ci) {
        if (this.dragonRespawnStage != null && this.respawnCrystals.contains(crystal)) {
            BetterEndIslandCommon.LOGGER.info("Aborting dragon respawn sequence");
            this.dragonRespawnStage = null;
            this.respawnTime = 0;
            this.resetSpikeCrystals();
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
                BetterEndIslandCommon.LOGGER.info("Tried to respawn, but need to find the portal first.");
                BlockPattern.BlockPatternMatch portalPatternMatch = this.findExitPortal();
                if (portalPatternMatch == null) {
                    BetterEndIslandCommon.LOGGER.info("Couldn't find a portal, so we made one.");
                    this.spawnPortal(false, false);
                    this.spawnPortal(true, true); // Place open, active bottom after spawning tower
                } else {
                    BetterEndIslandCommon.LOGGER.info("Found the exit portal & saved its location for next time.");
                }

                portalPos = this.portalLocation;
            }

            // Check for all 4 summoning crystals
            List<EndCrystal> allCrystals = this.checkRespawnCrystals(portalPos.above(1));
            if (allCrystals.size() != 4) return;

            BetterEndIslandCommon.LOGGER.info("Found all crystals, respawning dragon.");
            this.respawnDragon(allCrystals);
        }
        ci.cancel();
    }

    /**
     * Same as tryRespawn, but triggered by the player getting close enough.
     */
    @Unique
    @Override
    public void initialRespawn() {
        BlockPos portalPos = this.portalLocation;
        if (portalPos == null) {
            BetterEndIslandCommon.LOGGER.info("Tried to respawn, but need to find the portal first.");
            BlockPattern.BlockPatternMatch portalPatternMatch = this.findExitPortal();
            if (portalPatternMatch == null) {
                BetterEndIslandCommon.LOGGER.info("Couldn't find a portal, so we made one.");
                this.spawnPortal(false, false);
                this.spawnPortal(true, true); // Place open, active bottom after spawning tower
            } else {
                BetterEndIslandCommon.LOGGER.info("Found the exit portal & saved its location for next time.");
            }

            portalPos = this.portalLocation;
        }

        // Check for all 4 summoning crystals
        List<EndCrystal> allCrystals = this.checkRespawnCrystals(portalPos.above(1));
        if (allCrystals.size() != 4) return;

        BetterEndIslandCommon.LOGGER.info("Found all crystals, starting initial dragon spawn.");
        this.respawnDragon(allCrystals);
    }

    @Unique
    private List<EndCrystal> checkRespawnCrystals(BlockPos centerPos) {
        List<EndCrystal> foundCrystals = Lists.newArrayList();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            AABB crystalCheckbox = new AABB(centerPos.relative(direction, 7));
            List<EndCrystal> crystalsInDirection = this.level.getEntitiesOfClass(EndCrystal.class, crystalCheckbox);
            foundCrystals.addAll(crystalsInDirection);
        }

        return foundCrystals;
    }

    @Inject(method = "respawnDragon", at = @At("HEAD"), cancellable = true)
    private void betterendisland_respawnDragon(List<EndCrystal> crystals, CallbackInfo ci) {
        if ((this.dragonKilled || !this.hasDragonEverSpawned) && this.dragonRespawnStage == null) {
            this.dragonRespawnStage = DragonRespawnStage.START;
            this.respawnTime = 0;
            this.respawnCrystals = crystals;
        }
        ci.cancel();
    }

    @Unique
    private void spawnPortal(boolean isActive, boolean isBottomOnly) {
        // Find the portal location if it hasn't been found yet
        if (this.portalLocation == null) {
            this.portalLocation = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION).below();
            while (this.level.getBlockState(this.portalLocation).is(Blocks.BEDROCK) && this.portalLocation.getY() > this.level.getSeaLevel()) {
                this.portalLocation = this.portalLocation.below();
            }
        }

        BetterEndIslandCommon.LOGGER.info("Set the exit portal location to: {}", this.portalLocation);

        BetterEndPodiumFeature endPodiumFeature = new BetterEndPodiumFeature(this.firstExitPortalSpawn, isBottomOnly, isActive);
        BlockPos spawnPos = this.portalLocation.below(5);
        endPodiumFeature.place(FeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), spawnPos);
        this.firstExitPortalSpawn = false;
    }

    @Inject(method = "resetSpikeCrystals", at = @At("HEAD"), cancellable = true)
    public void betterendisland_resetSpikeCrystals(CallbackInfo ci) {
        // Vanilla logic - reset beam targets for crystals on spikes
        for(SpikeFeature.EndSpike $$0 : SpikeFeature.getSpikesForLevel(this.level)) {
            for(EndCrystal $$2 : this.level.getEntitiesOfClass(EndCrystal.class, $$0.getTopBoundingBox())) {
                $$2.setInvulnerable(false);
                $$2.setBeamTarget(null);
            }
        }

        // New logic - reset beam targets for summoning crystals.
        // This is necessary because the crystals aren't close enough to destroy each other when one is destroyed.
        if (this.respawnCrystals != null) {
            for (EndCrystal crystal : this.respawnCrystals) {
                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
            }
        }

        ci.cancel();
    }

    @Inject(method = "setDragonKilled", at = @At("HEAD"), cancellable = true)
    public void betterendisland_setDragonKilled(EnderDragon dragon, CallbackInfo ci) {
        if (dragon.getUUID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(0.0F);
            this.dragonEvent.setVisible(false);
            this.spawnPortal(true, true);
            level.explode(null, this.portalLocation.getX(), this.portalLocation.getY(), this.portalLocation.getZ(), 6.0F, Explosion.BlockInteraction.NONE);
            this.spawnNewGateway();
            if (!this.previouslyKilled) {
                this.level.setBlockAndUpdate(this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.END_PODIUM_LOCATION), Blocks.DRAGON_EGG.defaultBlockState());
            }
            this.previouslyKilled = true;
            this.dragonKilled = true;
            this.numberTimesDragonKilled++;
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
                // Create new dragon
                this.dragonRespawnStage = null;
                this.dragonKilled = false;
                EnderDragon newDragon = this.createNewDragon();
                for (ServerPlayer serverPlayer : this.dragonEvent.getPlayers()) {
                    CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, newDragon);
                }

                // Place broken tower w/ explosion effects
                this.spawnPortal(false, false);
                level.explode(null, this.portalLocation.getX(), this.portalLocation.getY() + 20, this.portalLocation.getZ(), 6.0F, Explosion.BlockInteraction.NONE);

                // Place open, inactive bottom if we're not transitioning from an initial tower
                if (this.hasDragonEverSpawned) {
                    this.spawnPortal(false, true);
                    level.explode(null, this.portalLocation.getX(), this.portalLocation.getY(), this.portalLocation.getZ(), 6.0F, Explosion.BlockInteraction.NONE);
                }

                this.hasDragonEverSpawned = true;
            } else {
                this.dragonRespawnStage = stage;
            }
        }
    }

    @Override
    public DragonRespawnStage getDragonRespawnStage() {
        return this.dragonRespawnStage;
    }

    @Override
    public boolean hasDragonEverSpawned() {
        return this.hasDragonEverSpawned;
    }

    @Override
    public int getNumberTimesDragonKilled() {
        return this.numberTimesDragonKilled;
    }
}
