package com.yungnickyoung.minecraft.betterendisland;

import net.fabricmc.api.ModInitializer;

public class BetterEndIslandFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BetterEndIslandCommon.init();
    }
}
