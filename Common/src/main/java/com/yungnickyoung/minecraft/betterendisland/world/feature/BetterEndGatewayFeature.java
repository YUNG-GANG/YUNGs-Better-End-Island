package com.yungnickyoung.minecraft.betterendisland.world.feature;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.processor.DragonEggProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.ObsidianProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

/**
 * A replacement of vanilla's EndGatewayFeature that uses structure randomized structure templates.
 * Injected via {@link com.yungnickyoung.minecraft.betterendisland.mixin.EndGatewayFeatureMixin}.
 */
public class BetterEndGatewayFeature {
    private static final List<StructureProcessor> PROCESSORS = List.of(
            new DragonEggProcessor()
    );

    public static boolean place(FeaturePlaceContext<EndGatewayConfiguration> ctx) {
        BlockPos origin = ctx.origin();
        WorldGenLevel level = ctx.level();
        EndGatewayConfiguration config = ctx.config();

        int numberTimesDragonKilled = 0;
        if (level instanceof ServerLevel serverLevel && serverLevel.getDragonFight() != null) {
            numberTimesDragonKilled = ((IDragonFight) serverLevel.getDragonFight()).betterendisland$numTimesDragonKilled();
        }

        ResourceLocation template = new ResourceLocation(BetterEndIslandCommon.MOD_ID, "gateway");
        boolean placed = placeTemplate(level, ctx.random(), origin, template, numberTimesDragonKilled);

        BlockPos portalPos = new BlockPos(origin);
        level.setBlock(portalPos, Blocks.END_GATEWAY.defaultBlockState(), 3);

        // Configure block entity for gateway
        config.getExit().ifPresent(exitPos -> {
            BlockEntity blockentity = level.getBlockEntity(portalPos);
            if (blockentity instanceof TheEndGatewayBlockEntity theendgatewayblockentity) {
                theendgatewayblockentity.setExitPosition(exitPos, config.isExitExact());
                blockentity.setChanged();
            }
        });

        return placed;
    }

    private static boolean placeTemplate(ServerLevelAccessor level, RandomSource randomSource, BlockPos centerPos, ResourceLocation id, int numberTimesDragonKilled) {
        Optional<StructureTemplate> templateOptional = level.getLevel().getStructureManager().get(id);
        if (templateOptional.isEmpty()) { // Unsuccessful creation. Name is probably invalid.
            BetterEndIslandCommon.LOGGER.warn("Failed to create invalid feature {}", id);
            return false;
        }

        // Create and place template
        StructureTemplate template = templateOptional.get();
        BlockPos cornerPos = centerPos.offset(-template.getSize().getX() / 2, -template.getSize().getY() / 2, -template.getSize().getZ() / 2);
        StructurePlaceSettings structurePlaceSettings = new StructurePlaceSettings();
        PROCESSORS.forEach(structurePlaceSettings::addProcessor);
        structurePlaceSettings.addProcessor(new ObsidianProcessor(numberTimesDragonKilled));
        structurePlaceSettings.setRotation(Rotation.NONE); // Structure is radially symmetrical so rotation doesn't matter
        structurePlaceSettings.setRotationPivot(new BlockPos(1, 0, 1));
        template.placeInWorld(level, cornerPos, centerPos, structurePlaceSettings, randomSource, 2);
        return true;
    }
}
