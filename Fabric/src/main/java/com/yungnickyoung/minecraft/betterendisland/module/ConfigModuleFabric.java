package com.yungnickyoung.minecraft.betterendisland.module;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.config.BEIConfigFabric;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.world.InteractionResult;

public class ConfigModuleFabric {
    public static void init() {
        AutoConfig.register(BEIConfigFabric.class, Toml4jConfigSerializer::new);
        AutoConfig.getConfigHolder(BEIConfigFabric.class).registerSaveListener(ConfigModuleFabric::bakeConfig);
        AutoConfig.getConfigHolder(BEIConfigFabric.class).registerLoadListener(ConfigModuleFabric::bakeConfig);
        bakeConfig(AutoConfig.getConfigHolder(BEIConfigFabric.class).get());
    }

    private static InteractionResult bakeConfig(ConfigHolder<BEIConfigFabric> configHolder, BEIConfigFabric configFabric) {
        bakeConfig(configFabric);
        return InteractionResult.SUCCESS;
    }

    private static void bakeConfig(BEIConfigFabric configFabric) {
        BetterEndIslandCommon.CONFIG.resummonedDragonDropsEgg = configFabric.general.resummonedDragonDropsEgg;
        BetterEndIslandCommon.CONFIG.useVanillaSpawnPlatform = configFabric.general.useVanillaSpawnPlatform;
    }
}
