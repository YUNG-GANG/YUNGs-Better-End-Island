package com.yungnickyoung.minecraft.betterendisland.mixin.accessor;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {
    @Accessor("exitPortalLocation")
    BlockPos getPortalLocation();

    @Accessor("exitPortalLocation")
    void setPortalLocation(BlockPos pos);

    @Invoker("createNewDragon")
    EnderDragon invokeCreateNewDragon();

    @Accessor("dragonKilled")
    void setDragonKilled(boolean killed);

    @Accessor("hasPreviouslyKilledDragon")
    boolean getPreviouslyKilled();

    @Accessor("dragonEvent")
    ServerBossEvent getDragonEvent();

    @Accessor("gateways")
    List<Integer> getGateways();
}
