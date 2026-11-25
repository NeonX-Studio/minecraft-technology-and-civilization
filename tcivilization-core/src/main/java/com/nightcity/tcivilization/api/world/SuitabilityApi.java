package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.annotation.AnyThread;
import net.minecraft.core.BlockPos;

/**
 * Core-hosted settlement site scoring, start-site scoring, soil parent, and corridor terrain cost contract (10 §5.18, §12.1).
 */
public interface SuitabilityApi {
    @AnyThread
    SiteScore citySiteScore(BlockPos pos);

    @AnyThread
    SiteScore startSiteScore(BlockPos pos);

    @AnyThread
    SoilParent soilParent(BlockPos pos);

    @AnyThread
    TerrainCost terrainCost(BlockPos from, BlockPos to, TransportMode mode);
}
