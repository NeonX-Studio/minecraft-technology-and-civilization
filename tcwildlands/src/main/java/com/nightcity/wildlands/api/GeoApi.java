package com.nightcity.wildlands.api;

import com.nightcity.tcivilization.api.world.HistoricalTrace;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import java.util.Locale;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;

public final class GeoApi {
    private static final GeoApi INSTANCE = new GeoApi();

    private GeoApi() {
    }

    public static GeoApi get() {
        return INSTANCE;
    }

    public void recordHistoricalTrace(
            String kindName,
            BlockPos pos,
            int radiusBlocks,
            int year,
            OptionalLong depositId,
            float minedFraction
    ) {
        HistoricalTrace.Kind kind = HistoricalTrace.Kind.valueOf(kindName.toUpperCase(Locale.ROOT));
        TcWorldApis.geo().recordHistoricalTrace(new HistoricalTrace(kind, pos, radiusBlocks, year, depositId, minedFraction));
    }

    public void recordHistoricalTrace(
            String kindName,
            BlockPos pos,
            int radiusBlocks,
            int year,
            long depositId,
            float minedFraction
    ) {
        OptionalLong opt = depositId >= 0L ? OptionalLong.of(depositId) : OptionalLong.empty();
        recordHistoricalTrace(kindName, pos, radiusBlocks, year, opt, minedFraction);
    }
}
