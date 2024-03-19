package com.yungnickyoung.minecraft.betterendisland.services;

import com.yungnickyoung.minecraft.betterendisland.module.ConfigModuleFabric;

public class FabricModulesLoader implements IModulesLoader {
    @Override
    public void loadModules() {
        IModulesLoader.super.loadModules(); // Load common modules
        ConfigModuleFabric.init();
    }
}
