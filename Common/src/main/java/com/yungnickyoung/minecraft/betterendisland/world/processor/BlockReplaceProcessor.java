package com.yungnickyoung.minecraft.betterendisland.world.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yungnickyoung.minecraft.yungsapi.api.world.randomize.BlockStateRandomizer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.NullMarked;

import javax.annotation.ParametersAreNonnullByDefault;


@NullMarked
public class BlockReplaceProcessor implements StructureProcessor {
    public static final MapCodec<BlockReplaceProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    BlockState.CODEC.fieldOf("target_block").forGetter(config -> config.targetBlock),
                    BlockStateRandomizer.CODEC.fieldOf("output").forGetter(config -> config.output),
                    Codec.BOOL.optionalFieldOf("randomize_facing", false).forGetter(config -> config.randomizeFacing),
                    Codec.BOOL.optionalFieldOf("randomize_half", false).forGetter(config -> config.randomizeHalf),
                    Codec.BOOL.optionalFieldOf("copy_input_properties", false).forGetter(config -> config.copyInputProperties),
                    Codec.BOOL.optionalFieldOf("preserve_waterlog", false).forGetter(config -> config.preserveWaterlog))
            .apply(instance, instance.stable(BlockReplaceProcessor::new)));

    public final BlockState targetBlock;
    public final BlockStateRandomizer output;
    public final boolean randomizeFacing;
    public final boolean randomizeHalf;
    public final boolean copyInputProperties;
    public final boolean preserveWaterlog;

    public BlockReplaceProcessor(BlockState targetBlock,
                                  BlockStateRandomizer output,
                                  boolean randomizeFacing,
                                  boolean randomizeHalf,
                                  boolean copyInputProperties,
                                  boolean preserveWaterlog) {
        this.targetBlock = targetBlock;
        this.output = output;
        this.randomizeFacing = randomizeFacing;
        this.randomizeHalf = randomizeHalf;
        this.copyInputProperties = copyInputProperties;
        this.preserveWaterlog = preserveWaterlog;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader,
                                                             BlockPos jigsawPiecePos,
                                                             BlockPos jigsawPieceBottomCenterPos,
                                                             BlockPos blockPos,
                                                             StructureTemplate.StructureBlockInfo blockInfo,
                                                             StructurePlaceSettings structurePlacementData) {
        if (blockInfo.state().is(this.targetBlock.getBlock())) {
            RandomSource random = structurePlacementData.getRandom(blockInfo.pos());
            BlockState outputState = output.get(random);

            if (this.copyInputProperties) {
                if (blockInfo.state().hasProperty(StairBlock.FACING) && outputState.hasProperty(StairBlock.FACING)) {
                    outputState = outputState.setValue(StairBlock.FACING, blockInfo.state().getValue(StairBlock.FACING));
                }
                if (blockInfo.state().hasProperty(StairBlock.HALF) && outputState.hasProperty(StairBlock.HALF)) {
                    outputState = outputState.setValue(StairBlock.HALF, blockInfo.state().getValue(StairBlock.HALF));
                }
                if (blockInfo.state().hasProperty(StairBlock.SHAPE) && outputState.hasProperty(StairBlock.SHAPE)) {
                    outputState = outputState.setValue(StairBlock.SHAPE, blockInfo.state().getValue(StairBlock.SHAPE));
                }
                if (blockInfo.state().hasProperty(SlabBlock.TYPE) && outputState.hasProperty(SlabBlock.TYPE)) {
                    outputState = outputState.setValue(SlabBlock.TYPE, blockInfo.state().getValue(SlabBlock.TYPE));
                }
                if (blockInfo.state().hasProperty(WallBlock.NORTH) && outputState.hasProperty(WallBlock.NORTH)) {
                    outputState = outputState.setValue(WallBlock.NORTH, blockInfo.state().getValue(WallBlock.NORTH));
                }
                if (blockInfo.state().hasProperty(WallBlock.EAST) && outputState.hasProperty(WallBlock.EAST)) {
                    outputState = outputState.setValue(WallBlock.EAST, blockInfo.state().getValue(WallBlock.EAST));
                }
                if (blockInfo.state().hasProperty(WallBlock.SOUTH) && outputState.hasProperty(WallBlock.SOUTH)) {
                    outputState = outputState.setValue(WallBlock.SOUTH, blockInfo.state().getValue(WallBlock.SOUTH));
                }
                if (blockInfo.state().hasProperty(WallBlock.WEST) && outputState.hasProperty(WallBlock.WEST)) {
                    outputState = outputState.setValue(WallBlock.WEST, blockInfo.state().getValue(WallBlock.WEST));
                }
                if (blockInfo.state().hasProperty(WallBlock.UP) && outputState.hasProperty(WallBlock.UP)) {
                    outputState = outputState.setValue(WallBlock.UP, blockInfo.state().getValue(WallBlock.UP));
                }

            }

            // Randomize output
            if (this.randomizeFacing) {
                if (outputState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    outputState = outputState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
                }
                if (outputState.hasProperty(BlockStateProperties.FACING)) {
                    outputState = outputState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.getRandom(random));
                }
            }
            if (this.randomizeHalf) {
                if (outputState.hasProperty(BlockStateProperties.HALF)) {
                    outputState = outputState.setValue(BlockStateProperties.HALF, random.nextBoolean() ? Half.TOP : Half.BOTTOM);
                }
                if (outputState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                    outputState = outputState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, random.nextBoolean() ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
                }
                if (outputState.hasProperty(BlockStateProperties.SLAB_TYPE)) {
                    outputState = outputState.setValue(BlockStateProperties.SLAB_TYPE, random.nextBoolean() ? SlabType.TOP : SlabType.BOTTOM);
                }
            }

            // Schedule fluid tick, if applicable
            if (levelReader instanceof WorldGenRegion worldGenRegion && (outputState.is(Blocks.WATER) || outputState.is(Blocks.LAVA))) {
                FlowingFluid fluid = outputState.is(Blocks.WATER) ? Fluids.WATER : Fluids.LAVA;
                worldGenRegion.scheduleTick(blockInfo.pos(), fluid, 0);
            }

            if (this.preserveWaterlog && outputState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && blockInfo.state().hasProperty(BlockStateProperties.WATERLOGGED) && blockInfo.state().getValue(BlockStateProperties.WATERLOGGED)) {
                outputState = outputState.setValue(BlockStateProperties.WATERLOGGED, true);
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
