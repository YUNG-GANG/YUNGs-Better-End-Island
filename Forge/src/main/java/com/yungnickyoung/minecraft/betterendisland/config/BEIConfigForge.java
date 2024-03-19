package com.yungnickyoung.minecraft.betterendisland.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class BEIConfigForge {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> resummonedDragonDropsEgg;
    public static final ForgeConfigSpec.ConfigValue<Boolean> useVanillaSpawnPlatform;

    static {
        BUILDER.push("YUNG's Better End Island");

        resummonedDragonDropsEgg = BUILDER
                .comment(
                        " Whether the Ender Dragon drops an egg when every time it's defeated.\n" +
                        " Default: false")
                .define("Resummoned Dragon Drops Egg", false);

        useVanillaSpawnPlatform = BUILDER
                .comment(
                        " Whether the vanilla obsidian platform should spawn in the End instead of the revamped platform.\n" +
                        " Default: false")
                .define("Spawn Vanilla Obsidian Platform", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
