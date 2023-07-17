package com.yungnickyoung.minecraft.betterendisland.world;

public interface IDragonFight {
    void betterendisland$setDragonRespawnStage(DragonRespawnStage stage);
    DragonRespawnStage betterendisland$getDragonRespawnStage();
    boolean betterendisland$hasDragonEverSpawned();
    void betterendisland$initialRespawn();
    int betterendisland$getNumberTimesDragonKilled();
    void betterendisland$tickBellSound();
    void betterendisland$reset();
}
