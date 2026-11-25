package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.annotation.AnyThread;
import com.nightcity.tcivilization.api.annotation.MainThread;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;

/**
 * Core-hosted hydrology, river reach, basin, and groundwater contract (10 §5.5, §12.1).
 */
public interface HydroApi {
    Optional<RiverInfo> river(BlockPos pos);

    @AnyThread
    GroundwaterInfo groundwater(BlockPos pos);

    @AnyThread
    int basinId(int x, int z);

    @AnyThread
    float floodRisk(BlockPos pos);

    @AnyThread
    Vec2 flowVelocity(BlockPos pos);

    @MainThread
    void registerWaterWork(WaterWorkRef ref);

    @MainThread
    void withdraw(BlockPos pos, float m3PerDay, Identifier consumer);
}
