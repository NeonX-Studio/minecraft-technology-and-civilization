package com.nightcity.tcivilization.api;

import com.nightcity.tcivilization.RegionCoordinates;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Server-side region grid, layer access, pinning, and pressure API (03 §5.2, §12.1).
 */
public interface RegionApi {
    static long regionKey(int rx, int rz) {
        return ((long) rx << 32) | (rz & 0xffff_ffffL);
    }

    static int wrapX(int x) {
        return RegionCoordinates.wrapX(x);
    }

    Optional<RegionHandle> at(ResourceKey<Level> dimension, BlockPos pos);

    Optional<RegionHandle> get(ResourceKey<Level> dimension, int rx, int rz);

    <T> T layer(RegionHandle region, RegionLayer<T> layer);

    <T> void update(RegionHandle region, RegionLayer<T> layer, UnaryOperator<T> operator);

    void pin(RegionHandle region, Object owner);

    void unpin(RegionHandle region, Object owner);

    float pressure(RegionHandle region);
}
