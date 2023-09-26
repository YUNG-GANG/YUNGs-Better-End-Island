package com.yungnickyoung.minecraft.betterendisland.world;

public interface IDragonFight {
    void betterendisland$setDragonRespawnStage(DragonRespawnStage stage);
    DragonRespawnStage betterendisland$getDragonRespawnStage();
    void betterendisland$initialRespawn();
    void betterendisland$tickBellSound();
    void betterendisland$reset(boolean forcePortalPosReset);
    void betterendisland$clearVanillaPillars();
    boolean betterendisland$firstExitPortalSpawn();
    void betterendisland$setFirstExitPortalSpawn(boolean bl);
    boolean betterendisland$hasDragonEverSpawned();
    void betterendisland$setHasDragonEverSpawned(boolean bl);
    int betterendisland$numTimesDragonKilled();
    void betterendisland$setNumTimesDragonKilled(int i);
}
