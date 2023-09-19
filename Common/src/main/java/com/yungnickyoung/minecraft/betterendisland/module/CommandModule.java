package com.yungnickyoung.minecraft.betterendisland.module;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.command.EndIslandCommand;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegisterCommand;

@AutoRegister(BetterEndIslandCommon.MOD_ID)
public class CommandModule {
    @AutoRegister("end_island")
    public static final AutoRegisterCommand END_ISLAND_COMMAND = AutoRegisterCommand.of(() -> EndIslandCommand::register);
}
