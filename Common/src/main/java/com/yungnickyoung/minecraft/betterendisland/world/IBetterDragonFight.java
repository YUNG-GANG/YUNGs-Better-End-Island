package com.yungnickyoung.minecraft.betterendisland.world;

import net.minecraft.world.level.dimension.end.EndDragonFight;

public interface IBetterDragonFight {
    // New data
    DragonRespawnStage getDragonRespawnStage();
    void setDragonRespawnStage(DragonRespawnStage stage);
    boolean isFirstExitPortalSpawn();
    void setIsFirstExitPortalSpawn(boolean bl);
    boolean hasDragonEverSpawned();
    void setHasDragonEverSpawned(boolean bl);
    int getNumTimesDragonKilled();
    void setNumTimesDragonKilled(int i);

    // Actions
    void advanceRespawnStage(DragonRespawnStage stage);
    void doInitialDragonSpawn();
    void tickBellSound();
    void reset(boolean forcePortalPosReset);
}
