package com.yungnickyoung.minecraft.betterendisland.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name="betterendisland-fabric-1_20_4")
public class BEIConfigFabric implements ConfigData {
    @ConfigEntry.Category("YUNG's Better End Island")
    @ConfigEntry.Gui.TransitiveObject
    public ConfigGeneralFabric general = new ConfigGeneralFabric();
}
