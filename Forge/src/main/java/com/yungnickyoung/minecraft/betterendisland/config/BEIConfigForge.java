package com.yungnickyoung.minecraft.betterendisland.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class BEIConfigForge {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> resummonedDragonDropsEgg;
    public static final ForgeConfigSpec.ConfigValue<Boolean> useVanillaSpawnPlatform;
    public static final ForgeConfigSpec.ConfigValue<Boolean> useVanillaEndGateways;
    public static final ForgeConfigSpec.ConfigValue<Boolean> playBellSound;
    public static final ForgeConfigSpec.ConfigValue<Boolean> spawnCentralTowerInitially;
    public static final ForgeConfigSpec.ConfigValue<Boolean> spawnCentralTowerOnResummon;

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

        spawnCentralTowerInitially = BUILDER
                .comment(
                        " Whether the central tower should spawn in the End when the world is first generated.\n" +
                        " Default: true")
                .define("Spawn Central Tower Initially", true);

        spawnCentralTowerOnResummon = BUILDER
                .comment(
                        " Whether the central tower should respawn in the End when the Ender Dragon is re-summoned.\n" +
                        " Default: true")
                .define("Respawn Central Tower on Resummon", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}