package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.DragonRespawnStage;
import com.yungnickyoung.minecraft.betterendisland.world.IBetterDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.IEndSpike;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndPodiumFeature;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndSpawnPlatformFeature;
import com.yungnickyoung.minecraft.betterendisland.world.util.EndCrystalUtils;
import com.yungnickyoung.minecraft.betterendisland.world.util.EndSpikeUtils;
import com.yungnickyoung.minecraft.betterendisland.world.util.ExitPortalUtils;
import com.yungnickyoung.minecraft.betterendisland.world.util.WorldgenUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.dimension.end.DragonRespawnAnimation;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
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
public abstract class EndDragonFightMixin implements IBetterDragonFight {
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

    @Shadow private int crystalsAlive;
    @Shadow @Nullable private DragonRespawnAnimation respawnStage;
    @Shadow @Final private ObjectArrayList<Integer> gateways;

    @Unique private DragonRespawnStage bei$dragonRespawnStage;
    @Unique private boolean bei$isFirstExitPortalSpawn = true;
    @Unique private boolean bei$hasDragonEverSpawned;
    @Unique private int bei$numTimesDragonKilled = 0;

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;JLnet/minecraft/world/level/dimension/end/EndDragonFight$Data;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
    public void betterendisland_EndDragonFight(ServerLevel level, long seed, EndDragonFight.Data data, BlockPos origin, CallbackInfo ci) {
        if (data.isRespawning()) {
            this.bei$dragonRespawnStage = DragonRespawnStage.START;
        }
        this.dragonEvent.setVisible(false);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void betterendisland_tickFight(CallbackInfo ci) {
        this.dragonEvent.setVisible(!this.dragonKilled && this.bei$hasDragonEverSpawned);
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
            if (this.bei$dragonRespawnStage != null) {
                if (this.respawnCrystals == null && isArenaLoaded) {
                    this.bei$dragonRespawnStage = null;
                    this.tryRespawn();
                }

                this.bei$dragonRespawnStage.tick(this.level, (EndDragonFight) (Object) this, this.respawnCrystals, this.respawnTime++);
            }

            if (!this.dragonKilled) {
                if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && isArenaLoaded && this.bei$hasDragonEverSpawned) {
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
    @Override
    public void reset(boolean forcePortalPosReset) {
        // Kill dragon if exists
        List<? extends EnderDragon> dragons = this.level.getDragons();
        dragons.forEach(EnderDragon::discard);
        this.dragonEvent.setProgress(0);
        this.dragonEvent.setVisible(false);

        // Get portal pos
        if (this.portalLocation == null || this.portalLocation.getY() < 5 || forcePortalPosReset) {
            BetterEndIslandCommon.LOGGER.info("Tried to reset, but need to find the portal first.");
            if (this.portalLocation == null) {
                BetterEndIslandCommon.LOGGER.info("Portal location is currently null.");
            } else if (this.portalLocation.getY() < 5) {
                BetterEndIslandCommon.LOGGER.info("Portal location is currently too low: {}", this.portalLocation.getY());
            } else {
                BetterEndIslandCommon.LOGGER.info("Forcing portal position reset...");
            }
            this.findExitPortal();
            if (this.portalLocation == null || this.portalLocation.getY() < 5 || forcePortalPosReset) { // If still null after finding portal, we find it ourselves
                if (this.portalLocation == null) {
                    BetterEndIslandCommon.LOGGER.info("Portal location is still null. Placing manually...");
                } else if (this.portalLocation.getY() < 5) {
                    BetterEndIslandCommon.LOGGER.info("Portal location is still too low: {}. Placing manually...", this.portalLocation.getY());
                }
                this.portalLocation = new BlockPos(0, WorldgenUtils.getSurfacePosAt(this.level, 0, 0), 0);
                while (this.level.getBlockState(this.portalLocation).is(Blocks.BEDROCK) && this.portalLocation.getY() > this.level.getSeaLevel()) {
                    this.portalLocation = this.portalLocation.below();
                }
                if (this.portalLocation.getY() < 5) {
                    BetterEndIslandCommon.LOGGER.info("Portal was still placed too low! Force placing at y=65...");
                    this.portalLocation = new BlockPos(this.portalLocation.getX(), 65, this.portalLocation.getZ());
                }
            }
        }

        // Reset vars to initial state
        this.dragonUUID = null;
        this.dragonKilled = false;
        this.previouslyKilled = false;
        this.bei$isFirstExitPortalSpawn = false;
        this.bei$hasDragonEverSpawned = false;
        this.bei$numTimesDragonKilled = 0;
        this.bei$dragonRespawnStage = null;
        this.respawnStage = null;
        this.respawnTime = 0;
        this.needsStateScanning = true;
        this.ticksSinceLastPlayerScan = 0;
        this.ticksSinceDragonSeen = 0;
        this.crystalsAlive = 0;
        this.ticksSinceCrystalsScanned = 0;

        // Get rid of summoning crystals
        if (this.respawnCrystals != null) {
            this.respawnCrystals.forEach(EndCrystal::discard);
        }
        this.respawnCrystals = null;
        List<EndCrystal> remainingSummoningCrystals = EndCrystalUtils.checkForBEIRespawnCrystals(this.level, this.portalLocation.above(1));
        remainingSummoningCrystals.forEach(EndCrystal::discard);
        remainingSummoningCrystals = EndCrystalUtils.checkForVanillaRespawnCrystals(this.level, this, this.portalLocation);
        remainingSummoningCrystals.forEach(EndCrystal::discard);

        // Get rid of spike crystals
        List<SpikeFeature.EndSpike> allSpikes = SpikeFeature.getSpikesForLevel(level);
        for (SpikeFeature.EndSpike spike : allSpikes) {
            for (EndCrystal crystal : this.level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox())) {
                crystal.discard();
            }
        }

        // Reset tower to initial state w/ summoning crystals
        if (BetterEndIslandCommon.CONFIG.spawnCentralTowerInitially) {
            // Spawn the central tower
            BetterEndPodiumFeature endPodiumFeature = new BetterEndPodiumFeature(true, false, false);
            BlockPos spawnPos = this.portalLocation.below(5);
            endPodiumFeature.place(FeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), spawnPos);
        } else {
            // Spawn the vanilla podium
            EndPodiumFeature endPodiumFeature = new EndPodiumFeature(false);
            if (endPodiumFeature.place(FeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), this.portalLocation)) {
                int $$2 = Mth.positiveCeilDiv(4, 16);
                this.level.getChunkSource().chunkMap.waitForLightBeforeSending(new ChunkPos(this.portalLocation), $$2);
            }
        }

        // Get rid of vanilla spikes in case they're there
        EndSpikeUtils.removeVanillaPillars(this.level);

        // Reset spikes to initial state
        EndSpikeUtils.resetSpikes(this.level, allSpikes);

        // Reset spawn platform to initial state
        BlockPos platformPos = ServerLevel.END_SPAWN_POINT.below();
        if (BetterEndIslandCommon.CONFIG.useVanillaSpawnPlatform) {
            EndPlatformFeature.createEndPlatform(level, platformPos, false);
        } else {
            BetterEndSpawnPlatformFeature.place(level, platformPos, false);
        }

        // Remove all gateways
        for (int i = 0; i < 20; i++) {
            int x = Mth.floor(96.0D * Math.cos(2.0D * (-Math.PI + 0.15707963267948966D * (double) i)));
            int z = Mth.floor(96.0D * Math.sin(2.0D * (-Math.PI + 0.15707963267948966D * (double) i)));
            BlockPos gatePos = new BlockPos(x, 75, z);
            BlockPos.betweenClosed(gatePos.offset(-1, -4, -1), gatePos.offset(1, 4, 1)).forEach(pos -> {
                this.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            });
        }

        // Reset gateways var
        this.gateways.clear();
        this.gateways.addAll(ContiguousSet.create(Range.closedOpen(0, 20), DiscreteDomain.integers()));
        Util.shuffle(this.gateways, RandomSource.create(this.level.getSeed()));
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
                ExitPortalUtils.spawnPortal(this, this.level, false, false);
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
        if (this.bei$dragonRespawnStage != null && this.respawnCrystals != null && this.respawnCrystals.contains(crystal)) {
            BetterEndIslandCommon.LOGGER.info("Aborting dragon respawn sequence");
            this.bei$dragonRespawnStage = null;
            this.respawnTime = 0;
            this.resetSpikeCrystals();
        } else {
            this.updateCrystalCount();
            Entity dragonEntity = this.level.getEntity(this.dragonUUID);
            if (dragonEntity instanceof EnderDragon) {
                ((EnderDragon) dragonEntity).onCrystalDestroyed(crystal, crystal.blockPosition(), damageSource);
            }
        }
        ci.cancel();
    }

    @Inject(method = "tryRespawn", at = @At("HEAD"), cancellable = true)
    public void betterendisland_tryRespawn(CallbackInfo ci) {
        if (this.dragonKilled && this.bei$dragonRespawnStage == null) {
            this.spawnDragon(false);
        }
        ci.cancel();
    }

    /**
     * Same as tryRespawn, but triggered by the player getting close enough.
     */
    @Unique
    @Override
    public void doInitialDragonSpawn() {
        BetterEndIslandCommon.LOGGER.info("Starting initial dragon fight!");
        this.spawnDragon(true);
    }

    @Unique
    private void spawnDragon(boolean isInitialSpawn) {
        BlockPos portalPos = this.portalLocation;
        if (portalPos == null) {
            BetterEndIslandCommon.LOGGER.info("Tried to respawn, but need to find the portal first.");
            BlockPattern.BlockPatternMatch portalPatternMatch = this.findExitPortal();
            if (portalPatternMatch == null) {
                BetterEndIslandCommon.LOGGER.info("Couldn't find a portal, so we made one.");
                ExitPortalUtils.spawnPortal(this, this.level, false, false);
                ExitPortalUtils.spawnPortal(this, this.level, true, true); // Place open, active bottom after spawning tower
            } else {
                BetterEndIslandCommon.LOGGER.info("Found the exit portal & saved its location for next time.");
            }

            portalPos = this.portalLocation;
        }

        // Check for all 4 summoning crystals
        List<EndCrystal> allCrystals = EndCrystalUtils.checkForBEIRespawnCrystals(this.level, portalPos.above(1));
        if (allCrystals.size() != 4) {
            allCrystals = EndCrystalUtils.checkForVanillaRespawnCrystals(this.level, this, portalPos);
            if (allCrystals.size() != 4) {
                if (isInitialSpawn) {
                    BetterEndIslandCommon.LOGGER.info("Unable to find all 4 summoning crystals. This shouldn't happen!");
                }
                return;
            }
        }

        if (isInitialSpawn) {
            BetterEndIslandCommon.LOGGER.info("Found all crystals, starting initial dragon spawn.");
        } else {
            BetterEndIslandCommon.LOGGER.info("Found all crystals, respawning dragon.");
        }
        this.respawnDragon(allCrystals);
    }

    @Inject(method = "respawnDragon", at = @At("HEAD"), cancellable = true)
    private void betterendisland_respawnDragon(List<EndCrystal> crystals, CallbackInfo ci) {
        if ((this.dragonKilled || !this.bei$hasDragonEverSpawned) && this.bei$dragonRespawnStage == null) {
            this.bei$dragonRespawnStage = DragonRespawnStage.START;
            this.respawnTime = 0;
            this.respawnCrystals = crystals;
        }
        ci.cancel();
    }

    @Inject(method = "resetSpikeCrystals", at = @At("RETURN"))
    public void betterendisland_resetSpikeCrystals(CallbackInfo ci) {
        // Reset beam targets for summoning crystals. This is necessary for BEI's crystals because unlike vanilla,
        // the crystals aren't close enough to destroy each other when one is destroyed.
        if (this.respawnCrystals != null) {
            for (EndCrystal crystal : this.respawnCrystals) {
                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
            }
        }
    }

    @Inject(method = "setDragonKilled", at = @At("HEAD"), cancellable = true)
    public void betterendisland_setDragonKilled(EnderDragon dragon, CallbackInfo ci) {
        if (dragon.getUUID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(0.0F);
            this.dragonEvent.setVisible(false);

            // Special case - killing the dragon for the first time with initial tower disabled but tower on resummoning enabled
            if (this.bei$numTimesDragonKilled == 0
                    && !BetterEndIslandCommon.CONFIG.spawnCentralTowerInitially
                    && BetterEndIslandCommon.CONFIG.spawnCentralTowerOnResummon) {
                ExitPortalUtils.spawnPortal(this, this.level, false, false);
            }
            ExitPortalUtils.spawnPortal(this, this.level, true, true);

            level.explode(null, this.portalLocation.getX(), this.portalLocation.getY(), this.portalLocation.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
            this.spawnNewGateway();
            if (!this.previouslyKilled || BetterEndIslandCommon.moreDragonEggs || BetterEndIslandCommon.CONFIG.resummonedDragonDropsEgg) {
                this.level.setBlockAndUpdate(this.portalLocation.above(), Blocks.DRAGON_EGG.defaultBlockState());
            }

            // Turn bedrock on spikes into obsidian
            int topY = BetterEndIslandCommon.betterEnd ? 70 : 60;
            List<SpikeFeature.EndSpike> spikes = SpikeFeature.getSpikesForLevel(level);
            spikes.forEach(spike -> {
                int crystalY = topY + ((IEndSpike) spike).getCrystalYOffset();
                level.setBlock(new BlockPos(spike.getCenterX(), crystalY - 1, spike.getCenterZ()), Blocks.OBSIDIAN.defaultBlockState(), 3);
            });

            this.previouslyKilled = true;
            this.dragonKilled = true;
            this.bei$numTimesDragonKilled++;
        }
        ci.cancel();
    }

    @Unique
    @Override
    public void advanceRespawnStage(DragonRespawnStage nextStage) {
        if (this.bei$dragonRespawnStage == null) {
            throw new IllegalStateException("Better Dragon respawn isn't in progress, can't skip ahead in the respawn process.");
        }
        this.respawnTime = 0;
        nextStage.onStart(this.level, this);
    }

    @Unique
    @Override
    public void tickBellSound() {
        if (!BetterEndIslandCommon.CONFIG.playBellSound) return;

        if (!this.bei$hasDragonEverSpawned || this.bei$dragonRespawnStage != null) {
            long gameTime = this.level.getGameTime();
            int soundY = this.portalLocation == null ? 80 : this.portalLocation.getY() + 15;

            if (gameTime % 100 == 0) {
                // Play bell sound every 4 seconds
                this.level.playSound(null, new BlockPos(0, soundY, 0), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 24.0f, 0.5f);

                // When close to center, play a higher pitch resonance sound
                this.level.playSound(null, new BlockPos(0, soundY, 0), SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 4.0f, 0.9f);
            }

            // Play low pitch resonance sound every 3 bell rings
            if (gameTime % 300 == 0) {
                this.level.playSound(null, new BlockPos(0, 80, 0), SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 24.0f, 0.8f);
            }
        }
    }

    @Unique
    @Override
    public DragonRespawnStage getDragonRespawnStage() {
        return this.bei$dragonRespawnStage;
    }

    @Unique
    @Override
    public boolean isFirstExitPortalSpawn() {
        return bei$isFirstExitPortalSpawn;
    }

    @Unique
    @Override
    public boolean hasDragonEverSpawned() {
        return bei$hasDragonEverSpawned;
    }

    @Unique
    @Override
    public int getNumTimesDragonKilled() {
        return bei$numTimesDragonKilled;
    }

    @Unique
    @Override
    public void setDragonRespawnStage(DragonRespawnStage stage) {
        this.bei$dragonRespawnStage = stage;
    }

    @Unique
    @Override
    public void setIsFirstExitPortalSpawn(boolean bl) {
        this.bei$isFirstExitPortalSpawn = bl;
    }

    @Unique
    @Override
    public void setHasDragonEverSpawned(boolean bl) {
        this.bei$hasDragonEverSpawned = bl;
    }

    @Unique
    @Override
    public void setNumTimesDragonKilled(int i) {
        this.bei$numTimesDragonKilled = i;
    }
}
