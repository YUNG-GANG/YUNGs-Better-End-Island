package com.yungnickyoung.minecraft.betterendisland;

import com.yungnickyoung.minecraft.betterendisland.services.Services;
import com.yungnickyoung.minecraft.yungsapi.api.YungAutoRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BetterEndIslandCommon {
    public static final String MOD_ID = "betterendisland";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init() {
        YungAutoRegister.scanPackageForAnnotations("com.yungnickyoung.minecraft.betterendisland.module");
        Services.MODULES.loadModules();
    }
}
