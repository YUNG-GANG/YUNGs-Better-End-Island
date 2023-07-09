package com.yungnickyoung.minecraft.betterendisland.world.feature;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.processor.BlockReplaceProcessor;
import com.yungnickyoung.minecraft.yungsapi.world.BlockStateRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

/**
 * A replacement of vanilla's EndPodiumFeature that uses a customized structure template.
 * Injected via {@link com.yungnickyoung.minecraft.betterendisland.mixin.EndPodiumFeatureMixin}.
 */
public class BetterEndPodiumFeature extends Feature<NoneFeatureConfiguration> {
    private static final List<StructureProcessor> PROCESSORS = List.of(
            new BlockReplaceProcessor(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    new BlockStateRandomizer(Blocks.BEDROCK.defaultBlockState()),
                    false, false, false, false)
    );

    private final boolean isInitialSpawn;

    public BetterEndPodiumFeature(boolean isInitialSpawn) {
        super(NoneFeatureConfiguration.CODEC);
        this.isInitialSpawn = isInitialSpawn;
    }

    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        ServerLevelAccessor level = ctx.level();
        RandomSource randomSource = ctx.random();
        BlockPos pos = ctx.origin();
        ResourceLocation template = chooseTemplate(randomSource, isInitialSpawn);
        Rotation rotation = Rotation.getRandom(randomSource);
        level.setBlock(pos.relative(Direction.NORTH, 16).relative(Direction.UP, 10), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        return placeTemplate(level, randomSource, pos, rotation, template);
    }

    private ResourceLocation chooseTemplate(RandomSource randomSource, boolean isInitialSpawn) {
        String towerType = "initial"; // TODO - add more tower types
        String towerName = "tower_" + towerType;
        return new ResourceLocation(BetterEndIslandCommon.MOD_ID, towerName);
    }

    private boolean placeTemplate(ServerLevelAccessor level, RandomSource randomSource, BlockPos centerPos, Rotation rotation, ResourceLocation id) {
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
        structurePlaceSettings.setRotation(rotation);
        structurePlaceSettings.setRotationPivot(new BlockPos(14, 0, 14));
        template.placeInWorld(level, cornerPos, centerPos, structurePlaceSettings, randomSource, 2);
        return true;
    }
}
