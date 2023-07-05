package com.yungnickyoung.minecraft.betterendisland.world;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;

import java.util.List;

public enum DragonRespawnStage {
    START {
        public void tick(ServerLevel level, EndDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer, BlockPos portalPos) {
            // Singular tick - update beam target pos for all crystals
            BlockPos beamTargetPos = new BlockPos(0, 128, 0);
            summoningCrystals.forEach(crystal -> crystal.setBeamTarget(beamTargetPos));
            ((IDragonFight) dragonFight).setDragonRespawnStage(PREPARING_TO_SUMMON_PILLARS);
        }
    },
    PREPARING_TO_SUMMON_PILLARS {
        public void tick(ServerLevel level, EndDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer, BlockPos portalPos) {
            // 100 ticks - growl sounds
            int totalPhaseTime = 100;
            if (phaseTimer < totalPhaseTime) {
                if (phaseTimer == 0 || phaseTimer == 50 || phaseTimer == 51 || phaseTimer == 52 || phaseTimer >= 95) {
                    broadcastDragonGrowlSound(level);
                }
            } else {
                ((IDragonFight) dragonFight).setDragonRespawnStage(SUMMONING_PILLARS);
            }
        }
    },
    SUMMONING_PILLARS {
        public void tick(ServerLevel level, EndDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer, BlockPos portalPos) {
            // Summons all spikes. 40 ticks per spike.
            int ticksPerSpike = 40;
            boolean isFirstTickForSpike = phaseTimer % ticksPerSpike == 0;
            boolean isLastTickForSpike = phaseTimer % ticksPerSpike == 39;
            if (isFirstTickForSpike || isLastTickForSpike) {
                List<SpikeFeature.EndSpike> allSpikes = SpikeFeature.getSpikesForLevel(level);
                int spikeIndex = phaseTimer / ticksPerSpike;
                if (spikeIndex < allSpikes.size()) {
                    SpikeFeature.EndSpike spike = allSpikes.get(spikeIndex);
                    if (isFirstTickForSpike) {
                        // On first tick for summoning a spike, set beam target for all crystals to point at the spike
                        for (EndCrystal crystal : summoningCrystals) {
                            crystal.setBeamTarget(new BlockPos(spike.getCenterX(), spike.getHeight() + 1, spike.getCenterZ()));
                        }
                    } else {
                        level.explode(null, (float) spike.getCenterX() + 0.5F, spike.getHeight(), (float) spike.getCenterZ() + 0.5F, 5.0F, Explosion.BlockInteraction.DESTROY);

                        int resetRadius = 10;
                        for (BlockPos blockPos : net.minecraft.core.BlockPos.betweenClosed(
                                new BlockPos(spike.getCenterX() - resetRadius, spike.getHeight() - 10, spike.getCenterZ() - resetRadius),
                                new BlockPos(spike.getCenterX() + resetRadius, spike.getHeight() + 25, spike.getCenterZ() + resetRadius))) {
                            level.removeBlock(blockPos, false);
                        }

                        // Place new spike
                        SpikeConfiguration spikeConfig = new SpikeConfiguration(true, ImmutableList.of(spike), new BlockPos(0, 128, 0));
                        Feature.END_SPIKE.place(spikeConfig, level, level.getChunkSource().getGenerator(), RandomSource.create(), new BlockPos(spike.getCenterX(), 45, spike.getCenterZ()));
                    }
                } else if (isFirstTickForSpike) {
                    ((IDragonFight) dragonFight).setDragonRespawnStage(SUMMONING_DRAGON);
                }
            }

        }
    },
    SUMMONING_DRAGON {
        public void tick(ServerLevel level, EndDragonFight dragonFight, List<EndCrystal> summoningCrystals, int phaseTimer, BlockPos portalPos) {
            int totalPhaseTime = 100;
            if (phaseTimer >= totalPhaseTime) {
                ((IDragonFight) dragonFight).setDragonRespawnStage(END);
                dragonFight.resetSpikeCrystals();

                for (EndCrystal crystal : summoningCrystals) {
                    crystal.setBeamTarget(null);
                    level.explode(crystal, crystal.getX(), crystal.getY(), crystal.getZ(), 6.0F, Explosion.BlockInteraction.NONE);
                    crystal.discard();
                }
            } else if (phaseTimer >= 80) {
                broadcastDragonGrowlSound(level);
            } else if (phaseTimer == 0) {
                for (EndCrystal crystal : summoningCrystals) {
                    crystal.setBeamTarget(new BlockPos(0, 128, 0));
                }
            } else if (phaseTimer < 5) {
                broadcastDragonGrowlSound(level);
            }

        }
    },
    END {
        public void tick(ServerLevel level, EndDragonFight dragonFight, List<EndCrystal> $$2, int phaseTimer, BlockPos $$4) {
        }
    };

    public abstract void tick(ServerLevel var1, EndDragonFight var2, List<EndCrystal> var3, int var4, BlockPos var5);

    private static void broadcastDragonGrowlSound(ServerLevel level) {
        level.levelEvent(3001, new BlockPos(0, 128, 0), 0);
    }
}
