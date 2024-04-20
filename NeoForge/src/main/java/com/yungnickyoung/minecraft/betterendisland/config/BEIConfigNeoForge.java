package com.yungnickyoung.minecraft.betterendisland.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BEIConfigNeoForge {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Boolean> resummonedDragonDropsEgg;
    public static final ModConfigSpec.ConfigValue<Boolean> useVanillaSpawnPlatform;

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