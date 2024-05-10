package com.yungnickyoung.minecraft.betterendisland.services;

import com.yungnickyoung.minecraft.betterendisland.module.ConfigModuleNeoForge;

public class NeoForgeModulesLoader implements IModulesLoader {
    @Override
    public void loadModules() {
        IModulesLoader.super.loadModules(); // Load common modules
        ConfigModuleNeoForge.init();
    }
}
