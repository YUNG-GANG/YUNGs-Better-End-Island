package com.yungnickyoung.minecraft.betterendisland.module;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.processor.BlockReplaceProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.DragonEggProcessor;
import com.yungnickyoung.minecraft.betterendisland.world.processor.ObsidianProcessor;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

@AutoRegister(BetterEndIslandCommon.MOD_ID)
public class StructureProcessorTypeModule {
    @AutoRegister("block_replace_processor")
    public static MapCodec<? extends StructureProcessor> BLOCK_REPLACE_PROCESSOR = BlockReplaceProcessor.CODEC;

    @AutoRegister("obsidian_processor")
    public static MapCodec<? extends StructureProcessor> OBSIDIAN_PROCESSOR = ObsidianProcessor.CODEC;

    @AutoRegister("dragon_egg_processor")
    public static MapCodec<? extends StructureProcessor> DRAGON_EGG_PROCESSOR = DragonEggProcessor.CODEC;
}
