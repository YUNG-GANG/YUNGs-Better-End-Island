package com.yungnickyoung.minecraft.betterendisland;

import com.yungnickyoung.minecraft.betterendisland.module.ConfigModuleNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(BetterEndIslandCommon.MOD_ID)
public class BetterEndIslandNeoForge {
    public static IEventBus loadingContextEventBus;

    public BetterEndIslandNeoForge(IEventBus eventBus, ModContainer container) {
        BetterEndIslandNeoForge.loadingContextEventBus = eventBus;

        BetterEndIslandCommon.init();
        ConfigModuleNeoForge.init(container);
    }
}