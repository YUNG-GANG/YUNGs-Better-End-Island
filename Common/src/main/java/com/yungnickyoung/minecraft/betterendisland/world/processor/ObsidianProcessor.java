package com.yungnickyoung.minecraft.betterendisland.world.processor;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.NullMarked;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Replaces obsidian with crying obsidian based on number of times dragon has been killed.
 */

@NullMarked
public class ObsidianProcessor implements StructureProcessor {
    public static final MapCodec<ObsidianProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
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
                                                             BlockPos blockPos,
                                                             StructureTemplate.StructureBlockInfo blockInfo,
                                                             StructurePlaceSettings structurePlacementData) {
        if (blockInfo.state().is(Blocks.OBSIDIAN)) {
            RandomSource random = structurePlacementData.getRandom(blockInfo.pos());
            BlockState outputState = Blocks.OBSIDIAN.defaultBlockState();
            int dragonKills = Mth.clamp(this.numberTimesDragonKilled, 0, 10);
            float cryingChance = Mth.lerp(dragonKills / 10f, 0f, 0.5f);
            if (random.nextFloat() < cryingChance) {
                outputState = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            }
            blockInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), outputState, blockInfo.nbt());
        }
        return blockInfo;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }
}
