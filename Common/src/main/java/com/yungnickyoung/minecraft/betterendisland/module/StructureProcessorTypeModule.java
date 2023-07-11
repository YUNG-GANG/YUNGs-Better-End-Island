package com.yungnickyoung.minecraft.betterendisland.module;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.processor.BlockReplaceProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.ObsidianProcessor;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

@AutoRegister(BetterEndIslandCommon.MOD_ID)
public class StructureProcessorTypeModule {
    @AutoRegister("block_replace_processor")
    public static StructureProcessorType<BlockReplaceProcessor> BLOCK_REPLACE_PROCESSOR = () -> BlockReplaceProcessor.CODEC;

    @AutoRegister("obsidian_processor")
    public static StructureProcessorType<ObsidianProcessor> OBSIDIAN_PROCESSOR = () -> ObsidianProcessor.CODEC;
}
