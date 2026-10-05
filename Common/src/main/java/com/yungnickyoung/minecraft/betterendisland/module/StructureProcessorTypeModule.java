package com.yungnickyoung.minecraft.betterendisland.module;

import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.processor.BlockReplaceProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.DragonEggProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.ObsidianProcessor;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;

@AutoRegister(BetterEndIslandCommon.MOD_ID)
public class StructureProcessorTypeModule {
    @AutoRegister("block_replace_processor")
    public static MapCodec<BlockReplaceProcessor> BLOCK_REPLACE_PROCESSOR = BlockReplaceProcessor.CODEC;

    @AutoRegister("obsidian_processor")
    public static MapCodec<ObsidianProcessor> OBSIDIAN_PROCESSOR = ObsidianProcessor.CODEC;

    @AutoRegister("dragon_egg_processor")
    public static MapCodec<DragonEggProcessor> DRAGON_EGG_PROCESSOR = DragonEggProcessor.CODEC;
}
