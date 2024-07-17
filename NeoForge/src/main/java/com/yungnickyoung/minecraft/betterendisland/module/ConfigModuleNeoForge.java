package com.yungnickyoung.minecraft.betterendisland.module;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandNeoForge;
import com.yungnickyoung.minecraft.betterendisland.config.BEIConfigNeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

public class ConfigModuleNeoForge {
    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, BEIConfigNeoForge.SPEC, "betterendisland-neoforge-1_21.toml");
        NeoForge.EVENT_BUS.addListener(ConfigModuleNeoForge::onWorldLoad);
        BetterEndIslandNeoForge.loadingContextEventBus.addListener(ConfigModuleNeoForge::onConfigChange);
    }

    private static void onWorldLoad(LevelEvent.Load event) {
        bakeConfig();
    }

    private static void onConfigChange(ModConfigEvent event) {
        if (event.getConfig().getSpec() == BEIConfigNeoForge.SPEC) {
            bakeConfig();
        }
    }

    private static void bakeConfig() {
        BetterEndIslandCommon.CONFIG.resummonedDragonDropsEgg = BEIConfigNeoForge.resummonedDragonDropsEgg.get();
        BetterEndIslandCommon.CONFIG.useVanillaSpawnPlatform = BEIConfigNeoForge.useVanillaSpawnPlatform.get();
    }
}
