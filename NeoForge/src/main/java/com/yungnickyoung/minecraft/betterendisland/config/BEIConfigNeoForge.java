package com.yungnickyoung.minecraft.betterendisland.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BEIConfigNeoForge {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Boolean> resummonedDragonDropsEgg;
    public static final ModConfigSpec.ConfigValue<Boolean> useVanillaSpawnPlatform;
    public static final ModConfigSpec.ConfigValue<Boolean> useVanillaEndGateways;
    public static final ModConfigSpec.ConfigValue<Boolean> playBellSound;

    static {
        BUILDER.push("YUNG's Better End Island");

        resummonedDragonDropsEgg = BUILDER
                .comment(
                        " Whether the Ender Dragon drops an egg every time it's defeated.\n" +
                        " Default: false")
                .define("Resummoned Dragon Drops Egg", false);

        useVanillaSpawnPlatform = BUILDER
                .comment(
                        " Whether the vanilla obsidian platform should spawn in the End instead of the revamped platform.\n" +
                        " Default: false")
                .define("Spawn Vanilla Obsidian Platform", false);

        useVanillaEndGateways = BUILDER
                .comment(
                        " Whether vanilla End Gateways should spawn in the End instead of the revamped End Gateways.\n" +
                        " Default: false")
                .define("Spawn Vanilla End Gateways", false);

        playBellSound = BUILDER
                .comment(
                        " Whether the bell sound should play before the Ender Dragon is summoned for the first time and during re-summonings.\n" +
                        " Default: true")
                .define("Play Bell Sound", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}