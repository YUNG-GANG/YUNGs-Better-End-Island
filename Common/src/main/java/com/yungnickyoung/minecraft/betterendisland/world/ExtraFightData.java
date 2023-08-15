package com.yungnickyoung.minecraft.betterendisland.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public class ExtraFightData {
    private boolean firstExitPortalSpawn;
    private boolean hasDragonEverSpawned;
    private int numTimesDragonKilled;

    public static final Codec<ExtraFightData> CODEC = RecordCodecBuilder.create((builder) -> builder.group(
                    Codec.BOOL.fieldOf("FirstExitPortalSpawn").orElse(true).forGetter(ExtraFightData::firstExitPortalSpawn),
                    Codec.BOOL.fieldOf("HasDragonEverSpawned").orElse(false).forGetter(ExtraFightData::hasDragonEverSpawned),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("NumberTimesDragonKilled").orElse(0).forGetter(ExtraFightData::numTimesDragonKilled))
            .apply(builder, ExtraFightData::new));

    public static final ExtraFightData DEFAULT = new ExtraFightData(true, false, 0);

    public ExtraFightData(boolean firstExitPortalSpawn, boolean hasDragonEverSpawned, int numTimesDragonKilled) {
        this.firstExitPortalSpawn = firstExitPortalSpawn;
        this.hasDragonEverSpawned = hasDragonEverSpawned;
        this.numTimesDragonKilled = numTimesDragonKilled;
    }

    public void setFirstExitPortalSpawn(boolean bl) {
        this.firstExitPortalSpawn = bl;
    }

    public boolean firstExitPortalSpawn() {
        return this.firstExitPortalSpawn;
    }

    public void setHasDragonEverSpawned(boolean bl) {
        this.hasDragonEverSpawned = bl;
    }

    public boolean hasDragonEverSpawned() {
        return this.hasDragonEverSpawned;
    }

    public void setNumTimesDragonKilled(int i) {
        this.numTimesDragonKilled = i;
    }

    public int numTimesDragonKilled() {
        return this.numTimesDragonKilled;
    }
}
