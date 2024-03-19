package com.yungnickyoung.minecraft.betterendisland.services;

import com.yungnickyoung.minecraft.betterendisland.module.ConfigModuleForge;

public class ForgeModulesLoader implements IModulesLoader {
    @Override
    public void loadModules() {
        IModulesLoader.super.loadModules(); // Load common modules
        ConfigModuleForge.init();
    }
}
