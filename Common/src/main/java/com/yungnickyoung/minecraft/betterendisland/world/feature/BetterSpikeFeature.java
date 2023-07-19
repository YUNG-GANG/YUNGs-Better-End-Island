package com.yungnickyoung.minecraft.betterendisland.world.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.mojang.datafixers.util.Pair;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.IEndSpike;
import com.yungnickyoung.minecraft.betterendisland.world.SpikeCacheLoader;
import com.yungnickyoung.minecraft.betterendisland.world.processor.BlockReplaceProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.DragonEggProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.ObsidianProcessor;
import com.yungnickyoung.minecraft.yungsapi.world.BlockStateRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A replacement of vanilla's SpikeFeature that uses structure randomized structure templates.
 * Injected via {@link com.yungnickyoung.minecraft.betterendisland.mixin.SpikeFeatureMixin}.
 */
public class BetterSpikeFeature {
    private static final LoadingCache<Long, List<SpikeFeature.EndSpike>> SPIKE_CACHE = CacheBuilder
            .newBuilder()
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build(new SpikeCacheLoader());

    private static final List<StructureProcessor> PROCESSORS = List.of(
            new BlockReplaceProcessor(
                    Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
                    new BlockStateRandomizer(Blocks.OBSIDIAN.defaultBlockState())
                            .addBlock(Blocks.CRYING_OBSIDIAN.defaultBlockState(), 0.3f),
                    false, false, false, false),
            new BlockReplaceProcessor(
                    Blocks.MAGENTA_TERRACOTTA.defaultBlockState(),
                    new BlockStateRandomizer(Blocks.AIR.defaultBlockState())
                            .addBlock(Blocks.CRYING_OBSIDIAN.defaultBlockState(), 0.1f)
                            .addBlock(Blocks.OBSIDIAN.defaultBlockState(), 0.1f),
                    false, false, false, false),
            new BlockReplaceProcessor(
                    Blocks.PURPLE_CONCRETE.defaultBlockState(),
                    new BlockStateRandomizer(Blocks.OBSIDIAN.defaultBlockState()),
                    false, false, false, false),
            new DragonEggProcessor()
    );

    public static List<SpikeFeature.EndSpike> getSpikesForLevel(WorldGenLevel level) {
        RandomSource randomSource = RandomSource.create(level.getSeed());
        long seed = randomSource.nextLong() & 65535L;
        return SPIKE_CACHE.getUnchecked(seed);
    }

    public static void placeSpike(ServerLevelAccessor level, RandomSource randomSource, SpikeConfiguration config, SpikeFeature.EndSpike spike, boolean isInitialSpawn) {
        // Choose templates based on spike and config.
        // First template ID is the top part, second is the bottom part.
        Pair<ResourceLocation, ResourceLocation> templates = chooseTemplates(spike, isInitialSpawn, randomSource.nextFloat() < 0.2f);

        // Use different random to ensure rotation for a given spike is same every fight
        long seed = 0;
        if (level instanceof WorldGenLevel) seed = ((WorldGenLevel) level).getSeed();
        RandomSource rand = RandomSource.create(seed ^ spike.getCenterX() ^ spike.getCenterZ());
        Rotation rotation = Rotation.getRandom(rand);

        int numberTimesDragonKilled = 0;
        if (level instanceof ServerLevel serverLevel && serverLevel.dragonFight() != null) {
            numberTimesDragonKilled = ((IDragonFight) serverLevel.dragonFight()).betterendisland$getNumberTimesDragonKilled();
        }

        // Place top part
        int topY = BetterEndIslandCommon.betterEnd ? 70 : 60;
        BlockPos centerPos = new BlockPos(spike.getCenterX(), topY, spike.getCenterZ());
        if (!placeTemplate(level, randomSource, centerPos, rotation, templates.getFirst(), numberTimesDragonKilled)) {
            BetterEndIslandCommon.LOGGER.error("Unable to place top spike at {}. This shouldn't happen!", centerPos);
            return;
        }

        // Place bottom part
        centerPos = centerPos.below(67);
        if (!placeTemplate(level, randomSource, centerPos, rotation, templates.getSecond(), numberTimesDragonKilled)) {
            BetterEndIslandCommon.LOGGER.error("Unable to place bottom spike at {}. This shouldn't happen!", centerPos);
            return;
        }

        // Spawn crystal and bedrock below it
        if (!isInitialSpawn) {
            EndCrystal endCrystal = EntityType.END_CRYSTAL.create(level.getLevel());
            endCrystal.setBeamTarget(config.getCrystalBeamTarget());
            endCrystal.setInvulnerable(config.isCrystalInvulnerable());
            int crystalY = topY + ((IEndSpike)spike).betterendisland$getCrystalYOffset();
            endCrystal.moveTo((double) spike.getCenterX() + 0.5D, crystalY, (double) spike.getCenterZ() + 0.5D, randomSource.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(endCrystal);
            level.setBlock(new BlockPos(spike.getCenterX(), crystalY - 1, spike.getCenterZ()), Blocks.BEDROCK.defaultBlockState(), 3);
        }
    }

    private static Pair<ResourceLocation, ResourceLocation> chooseTemplates(SpikeFeature.EndSpike spike, boolean isInitialSpawn, boolean isGuarded) {
        String pillarType = isInitialSpawn ? "initial" : (isGuarded ? "guarded" : "broken");
        int pillarHeight = (spike.getHeight() - 73) / 3;
        if (pillarHeight == 10) pillarHeight = 9; // We don't have a 10th variant
        String topName = "pillar_" + pillarType + "_" + pillarHeight;
        String bottomName = "pillar_bottom_" + pillarHeight;

        // Update spike crystal height, which depends on the template chosen.
        // This doesn't really belong here, but it's the easiest way to do it.
        ((IEndSpike) spike).betterendisland$setCrystalYOffsetFromPillarHeight(pillarHeight);

        return new Pair<>(new ResourceLocation(BetterEndIslandCommon.MOD_ID, topName), new ResourceLocation(BetterEndIslandCommon.MOD_ID, bottomName));
    }

    private static boolean placeTemplate(ServerLevelAccessor level, RandomSource randomSource, BlockPos centerPos, Rotation rotation, ResourceLocation id, int numberTimesDragonKilled) {
        Optional<StructureTemplate> templateOptional = level.getLevel().getStructureManager().get(id);
        if (templateOptional.isEmpty()) { // Unsuccessful creation. Name is probably invalid.
            BetterEndIslandCommon.LOGGER.warn("Failed to create invalid feature {}", id);
            return false;
        }

        // Create and place template
        StructureTemplate template = templateOptional.get();
        BlockPos cornerPos = centerPos.offset(-template.getSize().getX() / 2, 0, -template.getSize().getZ() / 2);
        StructurePlaceSettings structurePlaceSettings = new StructurePlaceSettings();
        PROCESSORS.forEach(structurePlaceSettings::addProcessor);
        structurePlaceSettings.addProcessor(new ObsidianProcessor(numberTimesDragonKilled));
        structurePlaceSettings.setRotation(rotation);
        structurePlaceSettings.setRotationPivot(new BlockPos(9, 0, 9));
        template.placeInWorld(level, cornerPos, centerPos, structurePlaceSettings, randomSource, 2);
        return true;
    }
}
