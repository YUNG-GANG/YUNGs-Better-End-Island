package com.yungnickyoung.minecraft.betterendisland.world;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.yungnickyoung.minecraft.betterendisland.BetterEndIslandCommon;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;

import java.util.List;
import java.util.stream.IntStream;

public class SpikeCacheLoader extends CacheLoader<Long, List<SpikeFeature.EndSpike>> {
    public List<SpikeFeature.EndSpike> load(Long $$0) {
        IntArrayList indexes = Util.toShuffledList(IntStream.range(0, 10), RandomSource.create($$0));
        List<SpikeFeature.EndSpike> spikes = Lists.newArrayList();
        double radius = BetterEndIslandCommon.betterEnd ? 42 : 54; // vanilla is 42.0

        for(int i = 0; i < 10; ++i) {
            int x = Mth.floor(radius * Math.cos(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
            int z = Mth.floor(radius * Math.sin(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
            int index = indexes.getInt(i);
            int pillarRadius = 2 + index / 3;
            int pillarHeight = 76 + index * 3;
            boolean isGuarded = index == 1 || index == 2;
            spikes.add(new SpikeFeature.EndSpike(x, z, pillarRadius, pillarHeight, isGuarded));
        }

        return spikes;
    }
}
