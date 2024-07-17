package com.yungnickyoung.minecraft.betterendisland.world.feature;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.processor.BlockReplaceProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.DragonEggProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.ObsidianProcessor;
import com.yungnickyoung.minecraft.yungsapi.api.world.randomize.BlockStateRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
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
 */
public class BetterEndPodiumFeature extends Feature<NoneFeatureConfiguration> {
    private static final List<StructureProcessor> PROCESSORS = List.of(
            new BlockReplaceProcessor(
                    Blocks.GRAY_CONCRETE.defaultBlockState(),
                    new BlockStateRandomizer(Blocks.BEDROCK.defaultBlockState()),
                    false, false, false, false),
            new DragonEggProcessor()
    );

    private static final StructureProcessor ACTIVE_PORTAL_PROCESSOR = new BlockReplaceProcessor(
            Blocks.RED_CONCRETE.defaultBlockState(),
            new BlockStateRandomizer(Blocks.END_PORTAL.defaultBlockState()),
            false, false, false, false);

    private static final StructureProcessor INACTIVE_PORTAL_PROCESSOR = new BlockReplaceProcessor(
            Blocks.RED_CONCRETE.defaultBlockState(),
            new BlockStateRandomizer(Blocks.AIR.defaultBlockState()),
            false, false, false, false);

    private final boolean isInitialSpawn;
    private final boolean isBottomOnly;
    private final boolean isActive;

    public BetterEndPodiumFeature(boolean isInitialSpawn, boolean isBottomOnly, boolean isActive) {
        super(NoneFeatureConfiguration.CODEC);
        this.isInitialSpawn = isInitialSpawn;
        this.isBottomOnly = isBottomOnly;
        this.isActive = isActive;
    }

    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        ServerLevelAccessor level = ctx.level();
        RandomSource randomSource = ctx.random();
        BlockPos pos = ctx.origin();

        int numberTimesDragonKilled = 0;
        if (level instanceof ServerLevel serverLevel && serverLevel.getDragonFight() != null) {
            numberTimesDragonKilled = ((IDragonFight) serverLevel.getDragonFight()).betterendisland$numTimesDragonKilled();
        }

        // Choose and place template
        ResourceLocation template = chooseTemplate();
        boolean placed = placeTemplate(level, randomSource, pos, Rotation.NONE, template, numberTimesDragonKilled);

        // Place crystals on initial spawn
        if (this.isInitialSpawn) {
            BlockPos centerPos = pos.above(6);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos crystalPos = centerPos.relative(direction, 8);
                EndCrystal crystal = new EndCrystal((Level) level, crystalPos.getX() + 0.5D, crystalPos.getY(), crystalPos.getZ() + 0.5D);
                crystal.setShowBottom(false);
                crystal.setInvulnerable(true); // Prevent player destroying crystals, which would result in a soft lock
                level.addFreshEntity(crystal);
            }
        }

        return placed;
    }

    private ResourceLocation chooseTemplate() {
        if (this.isBottomOnly) {
            return ResourceLocation.tryParse(BetterEndIslandCommon.MOD_ID + ":tower_bottom_open");
        }
        String towerType = this.isInitialSpawn ? "initial" : "broken";
        String towerName = "tower_" + towerType;
        return ResourceLocation.tryParse(BetterEndIslandCommon.MOD_ID + ":" + towerName);
    }

    private boolean placeTemplate(ServerLevelAccessor level, RandomSource randomSource, BlockPos centerPos, Rotation rotation, ResourceLocation id, int numberTimesDragonKilled) {
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
        if (this.isActive) {
            structurePlaceSettings.addProcessor(ACTIVE_PORTAL_PROCESSOR);
        } else {
            structurePlaceSettings.addProcessor(INACTIVE_PORTAL_PROCESSOR);
        }
        structurePlaceSettings.addProcessor(new ObsidianProcessor(numberTimesDragonKilled));
        structurePlaceSettings.setRotation(rotation);
        structurePlaceSettings.setRotationPivot(this.isBottomOnly ? new BlockPos(3, 0, 3) : new BlockPos(14, 0, 14));
        template.placeInWorld(level, cornerPos, centerPos, structurePlaceSettings, randomSource, 2);
        return true;
    }
}
