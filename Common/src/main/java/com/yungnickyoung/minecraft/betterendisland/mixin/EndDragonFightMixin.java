package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.DragonRespawnStage;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.IEndSpike;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndPodiumFeature;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterEndSpawnPlatformFeature;
import com.yungnickyoung.minecraft.betterendisland.world.feature.BetterSpikeFeature;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.end.DragonRespawnAnimation;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

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

    @Shadow private int crystalsAlive;
    @Shadow @Nullable private DragonRespawnAnimation respawnStage;
    @Shadow @Final private ObjectArrayList<Integer> gateways;

    @Unique private DragonRespawnStage betterendisland$dragonRespawnStage;
    @Unique private boolean betterendisland$firstExitPortalSpawn = true;
    @Unique private boolean betterendisland$hasDragonEverSpawned;
    @Unique private int betterendisland$numberTimesDragonKilled = 0;

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;JLnet/minecraft/world/level/dimension/end/EndDragonFight$Data;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
    public void betterendisland_EndDragonFight(ServerLevel level, long seed, EndDragonFight.Data data, BlockPos origin, CallbackInfo ci) {
        if (data.isRespawning()) {
            this.betterendisland$dragonRespawnStage = DragonRespawnStage.START;
        }
        this.dragonEvent.setVisible(false);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void betterendisland_tickFight(CallbackInfo ci) {
        this.dragonEvent.setVisible(!this.dragonKilled && this.betterendisland$hasDragonEverSpawned);
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
                this.betterendisland$scanForInitialState();
                this.needsStateScanning = false;
            }

            // Update respawn stage if performing respawn
            if (this.betterendisland$dragonRespawnStage != null) {
                if (this.respawnCrystals == null && isArenaLoaded) {
                    this.betterendisland$dragonRespawnStage = null;
                    this.tryRespawn();
                }

                this.betterendisland$dragonRespawnStage.tick(this.level, (EndDragonFight) (Object) this, this.respawnCrystals, this.respawnTime++, this.portalLocation);
            }

            if (!this.dragonKilled) {
                if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && isArenaLoaded && this.betterendisland$hasDragonEverSpawned) {
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

    @Override
    public void betterendisland$reset(boolean forcePortalPosReset) {
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
                this.portalLocation = new BlockPos(0, this.betterendisland$getSurfacePos(0, 0), 0);
                while (this.level.getBlockState(this.portalLocation).is(Blocks.BEDROCK) && this.portalLocation.getY() > this.level.getSeaLevel()) {
                    this.portalLocation = this.portalLocation.below();
                }
                if (this.portalLocation.getY() < 5) {
                    BetterEndIslandCommon.LOGGER.info("Portal was still placed too low! Force placing at y=65...");
                    this.portalLocation = new BlockPos(this.portalLocation.getX(), 65, this.portalLocation.getZ());
                }
            }
        }
        BlockPos portalPos = this.portalLocation;

        // Reset vars to initial state
        this.dragonUUID = null;
        this.dragonKilled = false;
        this.previouslyKilled = false;
        this.betterendisland$firstExitPortalSpawn = false;
        this.betterendisland$hasDragonEverSpawned = false;
        this.betterendisland$numberTimesDragonKilled = 0;
        this.betterendisland$dragonRespawnStage = null;
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
        List<EndCrystal> remainingSummoningCrystals = betterendisland$checkRespawnCrystals(portalPos.above(1));
        remainingSummoningCrystals.forEach(EndCrystal::discard);
        remainingSummoningCrystals = betterendisland$checkVanillaRespawnCrystals(portalPos.below(2));
        remainingSummoningCrystals.forEach(EndCrystal::discard);

        // Get rid of spike crystals
        List<SpikeFeature.EndSpike> allSpikes = SpikeFeature.getSpikesForLevel(level);
        for (SpikeFeature.EndSpike spike : allSpikes) {
            for (EndCrystal crystal : this.level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox())) {
                crystal.discard();
            }
        }

        // Reset tower to initial state w/ summoning crystals
        BetterEndPodiumFeature endPodiumFeature = new BetterEndPodiumFeature(true, false, false);
        BlockPos spawnPos = portalPos.below(5);
        endPodiumFeature.place(FeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), spawnPos);

        // Get rid of vanilla spikes in case they're there
        this.betterendisland$clearVanillaPillars();

        // Reset spikes to initial state
        allSpikes.forEach(spike -> {
            int resetRadius = 11;
            int verticalRadius = BetterEndIslandCommon.betterEnd ? 40 : 30;
            for (BlockPos blockPos : BlockPos.betweenClosed(
                    new BlockPos(spike.getCenterX() - resetRadius, spike.getHeight() - verticalRadius, spike.getCenterZ() - resetRadius),
                    new BlockPos(spike.getCenterX() + resetRadius, spike.getHeight() + verticalRadius, spike.getCenterZ() + resetRadius))) {
                if (!level.getBlockState(blockPos).is(Blocks.END_STONE)) {
                    level.removeBlock(blockPos, false);
                }
            }

            // Place new spike
            SpikeConfiguration spikeConfig = new SpikeConfiguration(true, ImmutableList.of(spike), null);
            BetterSpikeFeature.placeSpike(level, RandomSource.create(), spikeConfig, spike, true);
        });

        // Reset spawn playform to initial state
        BlockPos platformPos = ServerLevel.END_SPAWN_POINT.below();
        BetterEndSpawnPlatformFeature.place(level, platformPos);

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

    @Override
    public void betterendisland$clearVanillaPillars() {
        // Number of obsidian blocks removed.
        // We will only attempt to fill in holes in the terrain if the number of obsidian blocks removed surpasses a threshold.
        // That way we only fill in holes in the terrain if the vanilla pillars were removed.
        // We do this because filling in the holes is a last resort operation that can leave weird artifacts in the terrain.
        int obsidianRemoved = 0;

        // Copy vanilla logic to ensure we get the vanilla pillar logic, not something overwritten via mixin by a mod
        RandomSource randomSource = RandomSource.create(level.getSeed());
        long seed = randomSource.nextLong() & 65535L;
        IntArrayList indexes = Util.toShuffledList(IntStream.range(0, 10), RandomSource.create(seed));
        for (int i = 0; i < 10; ++i) {
            int x = Mth.floor(42.0D * Math.cos(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
            int z = Mth.floor(42.0D * Math.sin(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
            int index = indexes.get(i);
            int radius = 2 + index / 3;
            int height = 76 + index * 3;
            boolean isGuarded = index == 1 || index == 2;
            AABB topBoundingBox = new AABB(x - radius, DimensionType.MIN_Y, z - radius, x + radius, DimensionType.MAX_Y, z + radius);

            // Discard crystal on pillar if it exists
            this.level.getEntitiesOfClass(EndCrystal.class, topBoundingBox).forEach(EndCrystal::discard);

            // Remove obsidian & bedrock
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(x - radius, level.getMinBuildHeight(), z - radius), new BlockPos(x + radius, height + 20, z + radius))) {
                if (pos.distToLowCornerSqr(x, pos.getY(), z) <= (double)(radius * radius + 1)) {
                    BlockState blockState = level.getBlockState(pos);
                    if (blockState.is(Blocks.OBSIDIAN) || blockState.is(Blocks.BEDROCK)) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        if (blockState.is(Blocks.OBSIDIAN)) {
                            obsidianRemoved++;
                        }
                    }
                }
            }

            // Only fill in holes in terrain if we removed enough obsidian to warrant it
            if (obsidianRemoved > 10) {
                // Determine surface levels for pillar for filling in space in the island once we remove the spike
                int offset = radius + 1;
                int topY = -1;
                int surfaceY;
                if ((surfaceY = betterendisland$getSurfacePos(x - offset, z - offset)) > topY) topY = surfaceY;
                if ((surfaceY = betterendisland$getSurfacePos(x - offset, z + offset)) > topY) topY = surfaceY;
                if ((surfaceY = betterendisland$getSurfacePos(x + offset, z - offset)) > topY) topY = surfaceY;
                if ((surfaceY = betterendisland$getSurfacePos(x + offset, z + offset)) > topY) topY = surfaceY;
                int bottomY = 255;
                if ((surfaceY = betterendisland$getLowestBlockPos(x - offset, z - offset)) < bottomY) bottomY = surfaceY;
                if ((surfaceY = betterendisland$getLowestBlockPos(x - offset, z + offset)) < bottomY) bottomY = surfaceY;
                if ((surfaceY = betterendisland$getLowestBlockPos(x + offset, z - offset)) < bottomY) bottomY = surfaceY;
                if ((surfaceY = betterendisland$getLowestBlockPos(x + offset, z + offset)) < bottomY) bottomY = surfaceY;
                if (topY != -1 && bottomY != 255) {
                    for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(x - radius, bottomY, z - radius), new BlockPos(x + radius, topY, z + radius))) {
                        if (pos.distToLowCornerSqr(x, pos.getY(), z) <= (double) (radius * radius + 1)) {
                            BlockState blockState = level.getBlockState(pos);
                            if (blockState.is(Blocks.AIR)) {
                                if (pos.getY() <= topY && pos.getY() >= bottomY) {
                                    level.setBlockAndUpdate(pos, Blocks.END_STONE.defaultBlockState());
                                }
                            }
                        }
                    }
                }
            }

            // Remove iron fences
            if (isGuarded) {
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                for (int fenceX = -2; fenceX <= 2; ++fenceX) {
                    for (int fenceZ = -2; fenceZ <= 2; ++fenceZ) {
                        for (int fenceY = 0; fenceY <= 3; ++fenceY) {
                            if (Mth.abs(fenceX) == 2 || Mth.abs(fenceZ) == 2 || fenceY == 3) {
                                mutable.set(x + fenceX, height + fenceY, z + fenceZ);
                                level.setBlockAndUpdate(mutable, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }
    }

    @Unique
    private int betterendisland$getLowestBlockPos(int x, int z) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = this.level.getMinBuildHeight(); y < this.level.getMaxBuildHeight(); y++) {
            mutable.set(x, y, z);
            if (level.getBlockState(mutable).is(Blocks.END_STONE)) {
                return y;
            }
        }
        return 255;
    }

    @Unique
    private int betterendisland$getSurfacePos(int x, int z) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = this.level.getMaxBuildHeight(); y > this.level.getMinBuildHeight(); y--) {
            mutable.set(x, y, z);
            if (level.getBlockState(mutable).is(Blocks.END_STONE)) {
                return y;
            }
        }
        return -1;
    }

    @Unique
    private void betterendisland$scanForInitialState() {
        BetterEndIslandCommon.LOGGER.info("Scanning for legacy world dragon fight...");
        boolean hasActiveExitPortal = this.hasActiveExitPortal();
        if (hasActiveExitPortal) {
            BetterEndIslandCommon.LOGGER.info("Found that the dragon has been killed in this world already.");
            this.previouslyKilled = true;
        } else {
            BetterEndIslandCommon.LOGGER.info("Found that the dragon has not yet been killed in this world.");
            this.previouslyKilled = false;
            if (this.findExitPortal() == null) {
                this.betterendisland$spawnPortal(false, false);
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
        if (this.betterendisland$dragonRespawnStage != null && this.respawnCrystals.contains(crystal)) {
            BetterEndIslandCommon.LOGGER.info("Aborting dragon respawn sequence");
            this.betterendisland$dragonRespawnStage = null;
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
        if (this.dragonKilled && this.betterendisland$dragonRespawnStage == null) {
            BlockPos portalPos = this.portalLocation;
            if (portalPos == null) {
                BetterEndIslandCommon.LOGGER.info("Tried to respawn, but need to find the portal first.");
                BlockPattern.BlockPatternMatch portalPatternMatch = this.findExitPortal();
                if (portalPatternMatch == null) {
                    BetterEndIslandCommon.LOGGER.info("Couldn't find a portal, so we made one.");
                    this.betterendisland$spawnPortal(false, false);
                    this.betterendisland$spawnPortal(true, true); // Place open, active bottom after spawning tower
                } else {
                    BetterEndIslandCommon.LOGGER.info("Found the exit portal & saved its location for next time.");
                }

                portalPos = this.portalLocation;
            }

            // Check for all 4 summoning crystals
            List<EndCrystal> allCrystals = this.betterendisland$checkRespawnCrystals(portalPos.above(1));
            if (allCrystals.size() != 4) {
                allCrystals = this.betterendisland$checkVanillaRespawnCrystals(portalPos.below(2));
                if (allCrystals.size() != 4) {
                    return;
                }
            }

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
    public void betterendisland$initialRespawn() {
        BetterEndIslandCommon.LOGGER.info("Starting initial dragon fight!");

        BlockPos portalPos = this.portalLocation;
        if (portalPos == null) {
            BetterEndIslandCommon.LOGGER.info("Tried to respawn, but need to find the portal first.");
            BlockPattern.BlockPatternMatch portalPatternMatch = this.findExitPortal();
            if (portalPatternMatch == null) {
                BetterEndIslandCommon.LOGGER.info("Couldn't find a portal, so we made one.");
                this.betterendisland$spawnPortal(false, false);
                this.betterendisland$spawnPortal(true, true); // Place open, active bottom after spawning tower
            } else {
                BetterEndIslandCommon.LOGGER.info("Found the exit portal & saved its location for next time.");
            }

            portalPos = this.portalLocation;
        }

        // Check for all 4 summoning crystals
        List<EndCrystal> allCrystals = this.betterendisland$checkRespawnCrystals(portalPos.above(1));
        if (allCrystals.size() != 4) {
            allCrystals = this.betterendisland$checkVanillaRespawnCrystals(portalPos.below(2));
            if (allCrystals.size() != 4) {
                BetterEndIslandCommon.LOGGER.info("Unable to find all 4 summoning crystals. This shouldn't happen!");
                return;
            }
        }

        BetterEndIslandCommon.LOGGER.info("Found all crystals, starting initial dragon spawn.");
        this.respawnDragon(allCrystals);
    }

    @Unique
    private List<EndCrystal> betterendisland$checkRespawnCrystals(BlockPos centerPos) {
        List<EndCrystal> foundCrystals = Lists.newArrayList();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            AABB crystalCheckbox = new AABB(centerPos.relative(direction, 7));
            List<EndCrystal> crystalsInDirection = this.level.getEntitiesOfClass(EndCrystal.class, crystalCheckbox);
            foundCrystals.addAll(crystalsInDirection);
        }

        return foundCrystals;
    }

    @Unique
    private List<EndCrystal> betterendisland$checkVanillaRespawnCrystals(BlockPos centerPos) {
        List<EndCrystal> foundCrystals = Lists.newArrayList();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            AABB crystalCheckbox = new AABB(centerPos.relative(direction, 2));
            List<EndCrystal> crystalsInDirection = this.level.getEntitiesOfClass(EndCrystal.class, crystalCheckbox);
            foundCrystals.addAll(crystalsInDirection);
        }

        return foundCrystals;
    }

    @Inject(method = "respawnDragon", at = @At("HEAD"), cancellable = true)
    private void betterendisland_respawnDragon(List<EndCrystal> crystals, CallbackInfo ci) {
        if ((this.dragonKilled || !this.betterendisland$hasDragonEverSpawned) && this.betterendisland$dragonRespawnStage == null) {
            this.betterendisland$dragonRespawnStage = DragonRespawnStage.START;
            this.respawnTime = 0;
            this.respawnCrystals = crystals;
        }
        ci.cancel();
    }

    @Unique
    private void betterendisland$spawnPortal(boolean isActive, boolean isBottomOnly) {
        // Find the portal location if it hasn't been found yet
        if (this.portalLocation == null || this.portalLocation.getY() < 5) {
            if (this.portalLocation == null) {
                BetterEndIslandCommon.LOGGER.info("Portal location is null. Placing manually...");
            } else {
                BetterEndIslandCommon.LOGGER.info("Portal location is too low: {}. Placing manually...", this.portalLocation.getY());
            }
            this.portalLocation = new BlockPos(0, this.betterendisland$getSurfacePos(0, 0), 0);
            while (this.level.getBlockState(this.portalLocation).is(Blocks.BEDROCK) && this.portalLocation.getY() > this.level.getSeaLevel()) {
                this.portalLocation = this.portalLocation.below();
            }
            if (this.portalLocation.getY() < 5) {
                BetterEndIslandCommon.LOGGER.info("Portal was placed too low! Force placing at y=65...");
                this.portalLocation = new BlockPos(this.portalLocation.getX(), 65, this.portalLocation.getZ());
            }
        }

        BetterEndIslandCommon.LOGGER.info("Set the exit portal location to: {}", this.portalLocation);

        BetterEndPodiumFeature endPodiumFeature = new BetterEndPodiumFeature(this.betterendisland$firstExitPortalSpawn, isBottomOnly, isActive);
        BlockPos spawnPos = this.portalLocation.below(5);
        endPodiumFeature.place(FeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), spawnPos);
        this.betterendisland$firstExitPortalSpawn = false;
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
            this.betterendisland$spawnPortal(true, true);
            level.explode(null, this.portalLocation.getX(), this.portalLocation.getY(), this.portalLocation.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
            this.spawnNewGateway();
            if (!this.previouslyKilled || BetterEndIslandCommon.moreDragonEggs || BetterEndIslandCommon.CONFIG.resummonedDragonDropsEgg) {
                this.level.setBlockAndUpdate(this.portalLocation.above(), Blocks.DRAGON_EGG.defaultBlockState());
            }

            // Turn bedrock on spikes into obsidian
            int topY = BetterEndIslandCommon.betterEnd ? 70 : 60;
            List<SpikeFeature.EndSpike> spikes = SpikeFeature.getSpikesForLevel(level);
            spikes.forEach(spike -> {
                int crystalY = topY + ((IEndSpike)spike).betterendisland$getCrystalYOffset();
                level.setBlock(new BlockPos(spike.getCenterX(), crystalY - 1, spike.getCenterZ()), Blocks.OBSIDIAN.defaultBlockState(), 3);
            });

            this.previouslyKilled = true;
            this.dragonKilled = true;
            this.betterendisland$numberTimesDragonKilled++;
        }
        ci.cancel();
    }

    @Override
    public void betterendisland$setDragonRespawnStage(DragonRespawnStage stage) {
        if (this.betterendisland$dragonRespawnStage == null) {
            throw new IllegalStateException("Better Dragon respawn isn't in progress, can't skip ahead in the animation.");
        } else {
            this.respawnTime = 0;
            if (stage == DragonRespawnStage.END) {
                // Create new dragon
                this.betterendisland$dragonRespawnStage = null;
                this.dragonKilled = false;
                EnderDragon newDragon = this.createNewDragon();

                // Only trigger summon trigger (used for respawn advancement) if the dragon has been killed before,
                // since we auto-summon the dragon for the first fight
                if (this.previouslyKilled) {
                    for (ServerPlayer serverPlayer : this.dragonEvent.getPlayers()) {
                        CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, newDragon);
                    }
                }

                // Place broken tower w/ explosion effects
                this.betterendisland$spawnPortal(false, false);
                level.explode(null, this.portalLocation.getX(), this.portalLocation.getY() + 20, this.portalLocation.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
                level.players().forEach(player -> {
                    level.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, this.portalLocation.getX(), this.portalLocation.getY() + 20, this.portalLocation.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    if (player.distanceToSqr(this.portalLocation.getX(), this.portalLocation.getY() + 20, this.portalLocation.getZ()) > 32) {
                        level.playSound(null, this.portalLocation.above(20), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 24.0f, 1.0f);
                    }
                });
                // Place open, inactive bottom if we're not transitioning from an initial tower
                if (this.betterendisland$hasDragonEverSpawned) {
                    this.betterendisland$spawnPortal(false, true);
                    level.explode(null, this.portalLocation.getX(), this.portalLocation.getY(), this.portalLocation.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
                }

                // Update obsidian -> crying obsidian on all existing gateways
                int dragonKills = Mth.clamp(this.betterendisland$numberTimesDragonKilled, 0, 10);
                float cryingChance = Mth.lerp(dragonKills / 10f, 0f, 0.5f);
                List<Integer> existingGateways = new ArrayList<>(ContiguousSet.create(Range.closedOpen(0, 20), DiscreteDomain.integers()));
                existingGateways.removeAll(this.gateways);
                existingGateways.forEach(gateway -> {
                    int x = Mth.floor(96.0D * Math.cos(2.0D * (-Math.PI + 0.15707963267948966D * (double) gateway)));
                    int z = Mth.floor(96.0D * Math.sin(2.0D * (-Math.PI + 0.15707963267948966D * (double) gateway)));
                    BlockPos gatewayPos = new BlockPos(x, 75, z);
                    RandomSource gatewayRandom = RandomSource.create(Mth.getSeed(gatewayPos));
                    BlockPos.betweenClosed(gatewayPos.offset(-1, -4, -1), gatewayPos.offset(1, 4, 1)).forEach(pos -> {
                        if (level.getBlockState(pos).is(Blocks.OBSIDIAN) && gatewayRandom.nextFloat() < cryingChance) {
                            this.level.setBlockAndUpdate(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                        }
                    });
                });

                // Update obsidian -> crying obsidian on spawn platform
                BlockPos platformPos = ServerLevel.END_SPAWN_POINT;
                RandomSource platformRandom = RandomSource.create(Mth.getSeed(platformPos));
                BlockPos.betweenClosed(platformPos.offset(-3, -15, -3), platformPos.offset(3, 4, 3)).forEach(pos -> {
                    if (level.getBlockState(pos).is(Blocks.OBSIDIAN) && platformRandom.nextFloat() < cryingChance) {
                        this.level.setBlockAndUpdate(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    }
                });

                this.betterendisland$hasDragonEverSpawned = true;
            } else {
                this.betterendisland$dragonRespawnStage = stage;
            }
        }
    }

    @Override
    public void betterendisland$tickBellSound() {
        if (!this.betterendisland$hasDragonEverSpawned || this.betterendisland$dragonRespawnStage != null) {
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

    @Override
    public DragonRespawnStage betterendisland$getDragonRespawnStage() {
        return this.betterendisland$dragonRespawnStage;
    }

    @Override
    public boolean betterendisland$firstExitPortalSpawn() {
        return betterendisland$firstExitPortalSpawn;
    }

    @Override
    public boolean betterendisland$hasDragonEverSpawned() {
        return betterendisland$hasDragonEverSpawned;
    }

    @Override
    public int betterendisland$numTimesDragonKilled() {
        return betterendisland$numberTimesDragonKilled;
    }

    @Override
    public void betterendisland$setFirstExitPortalSpawn(boolean bl) {
        this.betterendisland$firstExitPortalSpawn = bl;
    }

    @Override
    public void betterendisland$setHasDragonEverSpawned(boolean bl) {
        this.betterendisland$hasDragonEverSpawned = bl;
    }

    @Override
    public void betterendisland$setNumTimesDragonKilled(int i) {
        this.betterendisland$numberTimesDragonKilled = i;
    }
}
