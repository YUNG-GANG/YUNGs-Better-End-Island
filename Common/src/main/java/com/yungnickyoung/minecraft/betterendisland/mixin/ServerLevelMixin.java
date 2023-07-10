package com.yungnickyoung.minecraft.betterendisland.mixin;

import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import com.yungnickyoung.minecraft.betterendisland.world.IDragonFight;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    @Shadow @Nullable public abstract EndDragonFight dragonFight();

    protected ServerLevelMixin(WritableLevelData $$0, ResourceKey<Level> $$1, Holder<DimensionType> $$2, Supplier<ProfilerFiller> $$3, boolean $$4, boolean $$5, long $$6, int $$7) {
        super($$0, $$1, $$2, $$3, $$4, $$5, $$6, $$7);
    }

    ResourceLocation END_DIMENSION = new ResourceLocation("minecraft", "the_end");

    @Inject(method = "tick", at = @At("HEAD"))
    private void betterendisland_tickInitialDragonSummonTrigger(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (this.dimension().location().equals(END_DIMENSION)
                && this.dragonFight() != null
                && !((IDragonFight) this.dragonFight()).hasDragonEverSpawned()
                && ((IDragonFight) this.dragonFight()).getDragonRespawnStage() == null
                && this.levelData.getGameTime() % 5 == 0)
        {
            double minDistance = -1.0D;
            double requiredDistance = 40.0D;
            Player foundPlayer = null;

            for (Player player : this.players()) {
                if (EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
                    double distance = xzDistanceSqr(0, 0, player.position().x(), player.position().z()); // (0, 0) is the center of the island
                    if (distance < requiredDistance * requiredDistance && (minDistance == -1.0D || distance < minDistance)) {
                        minDistance = distance;
                        foundPlayer = player;
                    }
                }
            }

            // Non-null foundPlayer means we found a player in range to trigger summoning the dragon
            if (foundPlayer != null) {
                BetterEndIslandCommon.LOGGER.info("Found player in range to summon dragon: {}", foundPlayer.getName().getString());
                ((IDragonFight) this.dragonFight()).initialRespawn();
            }
        }
    }

    @Unique
    private double xzDistanceSqr(double x1, double z1, double x2, double z2) {
        double xDist = x2 - x1;
        double zDist = z2 - z1;
        return xDist * xDist + zDist * zDist;
    }
}
