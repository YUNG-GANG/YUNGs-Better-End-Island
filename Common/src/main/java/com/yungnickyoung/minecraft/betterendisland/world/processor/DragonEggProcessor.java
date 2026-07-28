package com.yungnickyoung.minecraft.betterendisland.world.processor;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.NullMarked;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Avoids overwriting the dragon egg.
 */
@NullMarked
public class DragonEggProcessor implements StructureProcessor {
    public static final DragonEggProcessor INSTANCE = new DragonEggProcessor();
    public static final MapCodec<DragonEggProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader,
                                                             BlockPos jigsawPiecePos,
                                                             BlockPos jigsawPieceBottomCenterPos,
                                                             BlockPos blockPos,
                                                             StructureTemplate.StructureBlockInfo blockInfo,
                                                             StructurePlaceSettings structurePlacementData) {
        if (levelReader.getBlockState(blockInfo.pos()).is(Blocks.DRAGON_EGG)) {
            blockInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), Blocks.DRAGON_EGG.defaultBlockState(), blockInfo.nbt());
        }
        return blockInfo;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }
}
