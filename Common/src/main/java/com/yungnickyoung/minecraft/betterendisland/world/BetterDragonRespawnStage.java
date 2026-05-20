package com.yungnickyoung.minecraft.betterendisland.world;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.mixin.accessor.EnderDragonFightAccessor;
import com.yungnickyoung.minecraft.betterendisland.world.util.ExitPortalUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndSpikeConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public enum BetterDragonRespawnStage implements StringRepresentable {
    START("start") {
        @Override
        public void tick(ServerLevel serverLevel, EnderDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer) {
            // Singular tick - update beam target pos for all crystals
            BlockPos beamTargetPos = new BlockPos(0, 128, 0);
            summoningCrystals.forEach(crystal -> crystal.setBeamTarget(beamTargetPos));
            ((IBetterDragonFight) dragonFight).advanceRespawnStage(PREPARING_TO_SUMMON_PILLARS);
        }
    },
    PREPARING_TO_SUMMON_PILLARS("preparing_to_summon_pillars") {
        @Override
        public void tick(ServerLevel serverLevel, EnderDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer) {
            // 100 ticks - growl sounds
            int totalPhaseTime = 100;
            if (phaseTimer < totalPhaseTime) {
                if (phaseTimer == 0 || phaseTimer == 50 || phaseTimer == 51 || phaseTimer == 52 || phaseTimer >= 95) {
                    broadcastDragonGrowlSound(serverLevel);
                }
            } else {
                ((IBetterDragonFight) dragonFight).advanceRespawnStage(SUMMONING_PILLARS);
            }
        }
    },
    SUMMONING_PILLARS("summoning_pillars") {
        @Override
        public void tick(ServerLevel serverLevel, EnderDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer) {
            // Summons all spikes. 40 ticks per spike.
            int ticksPerSpike = 40;
            boolean isFirstTickForSpike = phaseTimer % ticksPerSpike == 0;
            boolean isLastTickForSpike = phaseTimer % ticksPerSpike == 39;
            if (isFirstTickForSpike || isLastTickForSpike) {
                List<EndSpikeFeature.EndSpike> allSpikes = EndSpikeFeature.getSpikesForLevel(serverLevel);
                int spikeIndex = phaseTimer / ticksPerSpike;
                if (spikeIndex < allSpikes.size()) {
                    EndSpikeFeature.EndSpike spike = allSpikes.get(spikeIndex);
                    int pillarHeight = (spike.getHeight() - 73) / 3;
                    if (pillarHeight == 10) pillarHeight = 9; // We don't have a 10th variant
                    ((IEndSpike) spike).setCrystalYOffsetFromPillarHeight(pillarHeight);
                    int topY = BetterEndIslandCommon.betterEnd ? 70 : 60; // Uses hardcoded topY; should be same as value in BetterSpikeFeature
                    int crystalY = topY + ((IEndSpike) spike).getCrystalYOffset() - 1;

                    if (isFirstTickForSpike) {
                        // On first tick for summoning a spike, set beam target for all crystals to point at the spike
                        for (EndCrystal crystal : summoningCrystals) {
                            crystal.setBeamTarget(new BlockPos(spike.getCenterX(), crystalY, spike.getCenterZ()));
                        }
                    } else {
                        serverLevel.explode(null, (float) spike.getCenterX() + 0.5F, crystalY, (float) spike.getCenterZ() + 0.5F, 5.0F, Level.ExplosionInteraction.BLOCK);
                        serverLevel.players().forEach(player -> {
                            serverLevel.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, true, spike.getCenterX() - 5, crystalY, spike.getCenterZ() - 5, 1, 0.0, 0.0, 0.0, 0.0);
                            serverLevel.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, true, spike.getCenterX() - 5, crystalY, spike.getCenterZ() + 5, 1, 0.0, 0.0, 0.0, 0.0);
                            serverLevel.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, true, spike.getCenterX() + 5, crystalY, spike.getCenterZ() - 5, 1, 0.0, 0.0, 0.0, 0.0);
                            serverLevel.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, true, spike.getCenterX() + 5, crystalY, spike.getCenterZ() + 5, 1, 0.0, 0.0, 0.0, 0.0);
                            if (player.distanceToSqr(spike.getCenterX(), crystalY, spike.getCenterZ()) > 32) {
                                serverLevel.playSound(null, new BlockPos(spike.getCenterX(), crystalY, spike.getCenterZ()), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 24.0f, 1.0f);
                            }
                        });

                        int resetRadius = 11;
                        int verticalRadius = BetterEndIslandCommon.betterEnd ? 40 : 30;
                        for (BlockPos blockPos : BlockPos.betweenClosed(
                                new BlockPos(spike.getCenterX() - resetRadius, spike.getHeight() - verticalRadius, spike.getCenterZ() - resetRadius),
                                new BlockPos(spike.getCenterX() + resetRadius, spike.getHeight() + verticalRadius, spike.getCenterZ() + resetRadius))) {
                            if (!serverLevel.getBlockState(blockPos).is(Blocks.END_STONE)) {
                                serverLevel.removeBlock(blockPos, false);
                            }
                        }

                        // Place new spike
                        EndSpikeConfiguration spikeConfig = new EndSpikeConfiguration(true, ImmutableList.of(spike), new BlockPos(0, 128, 0));
                        Feature.END_SPIKE.place(spikeConfig, serverLevel, serverLevel.getChunkSource().getGenerator(), RandomSource.create(), new BlockPos(spike.getCenterX(), 45, spike.getCenterZ()));
                    }
                } else if (isFirstTickForSpike) {
                    ((IBetterDragonFight) dragonFight).advanceRespawnStage(SUMMONING_DRAGON);
                }
            }

        }
    },
    SUMMONING_DRAGON("summoning_dragon") {
        @Override
        public void tick(ServerLevel serverLevel, EnderDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer) {
            int totalPhaseTime = 100;
            if (phaseTimer >= totalPhaseTime) { // Move to next stage after 100 ticks
                ((IBetterDragonFight) dragonFight).advanceRespawnStage(END);
                dragonFight.resetSpikeCrystals();

                for (EndCrystal crystal : summoningCrystals) {
                    crystal.setBeamTarget(null);
                    serverLevel.explode(crystal, crystal.getX(), crystal.getY(), crystal.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
                    crystal.discard();
                }
            } else if (phaseTimer >= 80) {
                broadcastDragonGrowlSound(serverLevel);
            } else if (phaseTimer == 0) { // Set beam target to the center of the island
                for (EndCrystal crystal : summoningCrystals) {
                    crystal.setBeamTarget(new BlockPos(0, 128, 0));
                }
            } else if (phaseTimer < 5) {
                broadcastDragonGrowlSound(serverLevel);
            }
        }
    },
    END("end") {
        @Override
        public void tick(ServerLevel serverLevel, EnderDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer) {
        }

        @Override
        public void onStart(ServerLevel serverLevel, IBetterDragonFight dragonFight) {
            BlockPos portalPos = ((EnderDragonFightAccessor) dragonFight).getPortalLocation();

            // Create new dragon
            dragonFight.setDragonRespawnStage(null);
            ((EnderDragonFightAccessor) dragonFight).setDragonKilled(false);
            EnderDragon newDragon = ((EnderDragonFightAccessor) dragonFight).invokeCreateNewDragon();

            // Only trigger summoning (used for respawn advancement) if the dragon has been killed before,
            // since we auto-summon the dragon for the first fight
            if (((EnderDragonFightAccessor) dragonFight).getPreviouslyKilled()) {
                for (ServerPlayer serverPlayer : ((EnderDragonFightAccessor) dragonFight).getDragonEvent().getPlayers()) {
                    CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, newDragon);
                }
            }

            // Place broken tower w/ explosion effects
            ExitPortalUtils.spawnPortal(dragonFight, serverLevel, false, false, true);
            serverLevel.explode(null, portalPos.getX(), portalPos.getY() + 20, portalPos.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
            serverLevel.players().forEach(player -> {
                serverLevel.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, true, portalPos.getX(), portalPos.getY() + 20, portalPos.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                if (player.distanceToSqr(portalPos.getX(), portalPos.getY() + 20, portalPos.getZ()) > 32) {
                    serverLevel.playSound(null, portalPos.above(20), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 24.0f, 1.0f);
                }
            });
            // Place open, inactive bottom if we're not transitioning from an initial tower
            if (dragonFight.hasDragonEverSpawned()) {
                ExitPortalUtils.spawnPortal(dragonFight, serverLevel, false, true, true);
                serverLevel.explode(null, portalPos.getX(), portalPos.getY(), portalPos.getZ(), 6.0F, Level.ExplosionInteraction.NONE);
            }

            // Update obsidian -> crying obsidian on all existing gateways
            int dragonKills = Mth.clamp(dragonFight.getNumTimesDragonKilled(), 0, 10);
            float cryingChance = Mth.lerp(dragonKills / 10f, 0f, 0.5f);
            List<Integer> existingGateways = new ArrayList<>(ContiguousSet.create(Range.closedOpen(0, 20), DiscreteDomain.integers()));
            existingGateways.removeAll(((EnderDragonFightAccessor) dragonFight).getGateways());
            existingGateways.forEach(gateway -> {
                int x = Mth.floor(96.0D * Math.cos(2.0D * (-Math.PI + 0.15707963267948966D * (double) gateway)));
                int z = Mth.floor(96.0D * Math.sin(2.0D * (-Math.PI + 0.15707963267948966D * (double) gateway)));
                BlockPos gatewayPos = new BlockPos(x, 75, z);
                RandomSource gatewayRandom = RandomSource.create(Mth.getSeed(gatewayPos));
                BlockPos.betweenClosed(gatewayPos.offset(-1, -4, -1), gatewayPos.offset(1, 4, 1)).forEach(pos -> {
                    if (serverLevel.getBlockState(pos).is(Blocks.OBSIDIAN) && gatewayRandom.nextFloat() < cryingChance) {
                        serverLevel.setBlockAndUpdate(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    }
                });
            });

            // Update obsidian -> crying obsidian on spawn platform
            BlockPos platformPos = ServerLevel.END_SPAWN_POINT;
            RandomSource platformRandom = RandomSource.create(Mth.getSeed(platformPos));
            BlockPos.betweenClosed(platformPos.offset(-3, -15, -3), platformPos.offset(3, 4, 3)).forEach(pos -> {
                if (serverLevel.getBlockState(pos).is(Blocks.OBSIDIAN) && platformRandom.nextFloat() < cryingChance) {
                    serverLevel.setBlockAndUpdate(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                }
            });

            dragonFight.setHasDragonEverSpawned(true);
        }
    };

    public static final StringRepresentable.EnumCodec<BetterDragonRespawnStage> CODEC = StringRepresentable.fromEnum(BetterDragonRespawnStage::values);

    @Nullable
    public static BetterDragonRespawnStage byName(@Nullable String name) {
        return CODEC.byName(name);
    }

    private final String name;

    BetterDragonRespawnStage(String name) {
        this.name = name.toLowerCase();
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    private static void broadcastDragonGrowlSound(ServerLevel level) {
        level.levelEvent(3001, new BlockPos(0, 128, 0), 0);
    }

    /**
     * Called every tick to update the state of the dragon respawn process.
     *
     * @param serverLevel       the ServerLevel
     * @param dragonFight       the EnderDragonFight instance
     * @param summoningCrystals the four EndCrystals that are being used to summon the dragon
     * @param phaseTimer        how many ticks have passed since the start of the current phase
     */
    public abstract void tick(ServerLevel serverLevel, EnderDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer);

    /**
     * Called when the stage is started, before the first tick.
     */
    public void onStart(ServerLevel serverLevel, IBetterDragonFight dragonFight) {
        dragonFight.setDragonRespawnStage(this);
    }
}
