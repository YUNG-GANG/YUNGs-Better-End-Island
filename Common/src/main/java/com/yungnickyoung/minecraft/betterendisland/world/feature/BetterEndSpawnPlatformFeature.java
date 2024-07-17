package com.yungnickyoung.minecraft.betterendisland.world.feature;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.mixin.EndPlatformFeatureMixin;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import com.yungnickyoung.minecraft.betterendisland.world.processor.DragonEggProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.ObsidianProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

/**
 * A replacement of vanilla's obsidian spawn platform that uses structure randomized structure templates.
 * Injected via {@link EndPlatformFeatureMixin}.
 */
public class BetterEndSpawnPlatformFeature {
    private static final List<StructureProcessor> PROCESSORS = List.of(
            new DragonEggProcessor()
    );

    public static boolean place(ServerLevel level, BlockPos pos) {
        BlockPos origin = pos.offset(0, -14, 0);
        int numberTimesDragonKilled = 0;
        if (level.getDragonFight() != null) {
            numberTimesDragonKilled = ((IDragonFight) level.getDragonFight()).betterendisland$numTimesDragonKilled();
        }
        ResourceLocation template = ResourceLocation.tryParse(BetterEndIslandCommon.MOD_ID + ":spawn_platform");
        return placeTemplate(level, RandomSource.create(), origin, template, numberTimesDragonKilled);
    }

    private static boolean placeTemplate(ServerLevelAccessor level, RandomSource randomSource, BlockPos centerPos, ResourceLocation id, int numberTimesDragonKilled) {
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
        structurePlaceSettings.setRotation(Rotation.NONE); // Structure is radially symmetrical so rotation doesn't matter
        structurePlaceSettings.setRotationPivot(new BlockPos(3, 0, 3));
        template.placeInWorld(level, cornerPos, centerPos, structurePlaceSettings, randomSource, 2);
        return true;
    }
}
