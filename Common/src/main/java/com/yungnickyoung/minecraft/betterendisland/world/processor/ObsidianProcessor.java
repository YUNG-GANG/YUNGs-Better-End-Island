package com.yungnickyoung.minecraft.betterendisland.world.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yungnickyoung.minecraft.betterendisland.module.StructureProcessorTypeModule;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Replaces obsidian with crying obsidian based on number of times dragon has been killed.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ObsidianProcessor extends StructureProcessor {
    public static final Codec<ObsidianProcessor> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("number_times_dragon_killed").forGetter(config -> config.numberTimesDragonKilled))
            .apply(instance, instance.stable(ObsidianProcessor::new)));

    private final int numberTimesDragonKilled;

    public ObsidianProcessor(int numberTimesDragonKilled) {
        this.numberTimesDragonKilled = numberTimesDragonKilled;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader,
                                                             BlockPos jigsawPiecePos,
                                                             BlockPos jigsawPieceBottomCenterPos,
                                                             StructureTemplate.StructureBlockInfo blockInfoLocal,
                                                             StructureTemplate.StructureBlockInfo blockInfoGlobal,
                                                             StructurePlaceSettings structurePlacementData) {
        if (blockInfoGlobal.state().is(Blocks.OBSIDIAN)) {
            RandomSource random = structurePlacementData.getRandom(blockInfoGlobal.pos());
            BlockState outputState = Blocks.OBSIDIAN.defaultBlockState();
            int dragonKills = Mth.clamp(this.numberTimesDragonKilled, 0, 10);
            float cryingChance = Mth.lerp(dragonKills / 10f, 0f, 0.5f);
            if (random.nextFloat() < cryingChance) {
                outputState = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            }
            blockInfoGlobal = new StructureTemplate.StructureBlockInfo(blockInfoGlobal.pos(), outputState, blockInfoGlobal.nbt());
        }
        return blockInfoGlobal;
    }

    protected StructureProcessorType<?> getType() {
        return StructureProcessorTypeModule.OBSIDIAN_PROCESSOR;
    }
}
