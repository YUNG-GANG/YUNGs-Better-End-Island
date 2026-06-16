package com.yungnickyoung.minecraft.betterendisland.world;

public interface IBetterDragonFight {
    // New data
    BetterDragonRespawnStage getDragonRespawnStage();
    void setDragonRespawnStage(BetterDragonRespawnStage stage);
    boolean isFirstExitPortalSpawn();
    void setIsFirstExitPortalSpawn(boolean bl);
    boolean hasDragonEverSpawned();
    void setHasDragonEverSpawned(boolean bl);
    int getNumTimesDragonKilled();
    void setNumTimesDragonKilled(int i);

    // Actions
    void advanceRespawnStage(BetterDragonRespawnStage stage);
    void doInitialDragonSpawn();
    void tickBellSound();
    void reset(boolean forcePortalPosReset);
}
