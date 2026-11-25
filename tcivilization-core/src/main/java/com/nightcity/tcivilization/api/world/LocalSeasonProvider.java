package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Resolves hemisphere-aware and regime-aware local season (10 §5.12.1, §12.1).
 */
public interface LocalSeasonProvider {
    LocalSeason season(@Nullable Level level, BlockPos pos, long dayIndex);

    default LocalSeason season(BlockPos pos, long dayIndex) {
        return season(null, pos, dayIndex);
    }

    default Season season(BlockPos pos) {
        return season(null, pos, 0L).effectiveSeason();
    }
}
