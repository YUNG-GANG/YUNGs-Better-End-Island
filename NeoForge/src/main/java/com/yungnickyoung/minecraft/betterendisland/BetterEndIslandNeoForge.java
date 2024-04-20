package com.yungnickyoung.minecraft.betterendisland;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(BetterEndIslandCommon.MOD_ID)
public class BetterEndIslandNeoForge {
    public static IEventBus loadingContextEventBus;

    public BetterEndIslandNeoForge(IEventBus eventBus) {
        BetterEndIslandNeoForge.loadingContextEventBus = eventBus;

        BetterEndIslandCommon.init();
    }
}