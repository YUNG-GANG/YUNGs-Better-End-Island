package com.yungnickyoung.minecraft.betterendisland.world;

public interface IDragonFight {
    void setDragonRespawnStage(DragonRespawnStage stage);
    DragonRespawnStage getDragonRespawnStage();
    boolean hasDragonEverSpawned();
    void initialRespawn();
    int getNumberTimesDragonKilled();
    void tickBellSound();
}
