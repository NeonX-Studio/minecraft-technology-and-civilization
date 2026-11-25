package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.annotation.AnyThread;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Core-hosted planetary coordinate, elevation, astronomy, and coarse surface query contract (10 §5.1, §12.1).
 */
public interface PlanetApi {
    int PLANET_WIDTH = 163_840;
    int PLANET_HEIGHT = 81_920;
    int X_MIN = -81_920;
    int X_MAX = 81_920;
    int Z_MIN = -40_960;
    int Z_MAX = 40_960;
    int Y_MIN = -512;
    int Y_MAX = 1024;
    int SEA_LEVEL = 63;
    int WRAP_BUFFER = 256;
    int TELEPORT_AT = X_MAX + 128;
    double METERS_PER_BLOCK = 40_075_000.0 / PLANET_WIDTH;

    @AnyThread
    boolean isPlanetActive();

    @AnyThread
    int planetWidth();

    @AnyThread
    int planetHeight();

    @AnyThread
    LatLon latLon(int x, int z);

    @AnyThread
    BlockPos toBlock(double latDeg, double lonDeg, int y);

    @AnyThread
    int wrapX(int x);

    @AnyThread
    int wrappedDx(int fromX, int toX);

    @AnyThread
    double wrappedDistance(BlockPos a, BlockPos b);

    @AnyThread
    double altitudeMeters(double y);

    @AnyThread
    double yForAltitude(double meters);

    @AnyThread
    int surfaceY(int x, int z);

    @AnyThread
    Identifier biomeAt(int x, int z);

    @AnyThread
    float dayLengthHours(double latDeg, int dayOfYear);

    @AnyThread
    float solarElevationDeg(BlockPos pos, long gameTime);

    @AnyThread
    PlanetSummary summary();
}
