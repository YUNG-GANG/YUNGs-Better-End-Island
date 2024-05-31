package com.yungnickyoung.minecraft.betterendisland.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class ConfigGeneralFabric {
    @ConfigEntry.Gui.Tooltip
    public boolean resummonedDragonDropsEgg = false;

    @ConfigEntry.Gui.Tooltip
    public boolean useVanillaSpawnPlatform = false;

    @ConfigEntry.Gui.Tooltip
    public boolean regenerateTowerOnDragonDeath = false;
}