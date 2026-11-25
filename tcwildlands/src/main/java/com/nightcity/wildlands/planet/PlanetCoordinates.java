package com.nightcity.wildlands.planet;

import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.world.LatLon;
import com.nightcity.tcivilization.api.world.PlanetApi;
import net.minecraft.core.BlockPos;

/**
 * Planetary coordinate projection, east-west wrap arithmetic, nonlinear vertical mapping, and solar geometry (10 §5.1).
 */
public final class PlanetCoordinates {
    public static final int PLANET_WIDTH = PlanetApi.PLANET_WIDTH;
    public static final int PLANET_HEIGHT = PlanetApi.PLANET_HEIGHT;
    public static final int X_MIN = PlanetApi.X_MIN;
    public static final int X_MAX = PlanetApi.X_MAX;
    public static final int Z_MIN = PlanetApi.Z_MIN;
    public static final int Z_MAX = PlanetApi.Z_MAX;
    public static final int Y_MIN = PlanetApi.Y_MIN;
    public static final int Y_MAX = PlanetApi.Y_MAX;
    public static final int SEA_LEVEL = PlanetApi.SEA_LEVEL;
    public static final int WRAP_BUFFER = PlanetApi.WRAP_BUFFER;
    public static final int TELEPORT_AT = PlanetApi.TELEPORT_AT;
    public static final int WORLD_BORDER_HALF = X_MAX + WRAP_BUFFER;
    public static final double METERS_PER_BLOCK = PlanetApi.METERS_PER_BLOCK;
    public static final double VERTICAL_GAMMA = 0.8;

    public static final int L0_STEP = 64;
    public static final int L0_WIDTH = PLANET_WIDTH / L0_STEP;     // 2560
    public static final int L0_HEIGHT = PLANET_HEIGHT / L0_STEP;   // 1280
    public static final int CLIM_STEP = 128;
    public static final int CLIM_WIDTH = PLANET_WIDTH / CLIM_STEP; // 1280
    public static final int CLIM_HEIGHT = PLANET_HEIGHT / CLIM_STEP; // 640

    private PlanetCoordinates() {
    }

    public static int wrapX(int x) {
        long shifted = (long) x - X_MIN;
        return (int) Math.floorMod(shifted, (long) PLANET_WIDTH) + X_MIN;
    }

    public static double wrapXDouble(double x) {
        double shifted = x - X_MIN;
        double mod = shifted - Math.floor(shifted / PLANET_WIDTH) * PLANET_WIDTH;
        return mod + X_MIN;
    }

    public static int clampZ(int z) {
        return Math.clamp(z, Z_MIN, Z_MAX - 1);
    }

    public static int wrappedDx(int fromX, int toX) {
        long raw = (long) wrapX(toX) - (long) wrapX(fromX);
        long wrapped = Math.floorMod(raw + X_MAX, (long) PLANET_WIDTH) - X_MAX;
        return (int) wrapped;
    }

    public static double wrappedDistance(BlockPos a, BlockPos b) {
        double dx = wrappedDx(a.getX(), b.getX());
        double dz = b.getZ() - a.getZ();
        return Math.hypot(dx, dz);
    }

    public static LatLon latLon(int x, int z) {
        int wx = wrapX(x);
        double lat = Math.clamp((-z / (double) Z_MAX) * 90.0, -90.0, 90.0);
        double lon = (wx / (double) X_MAX) * 180.0;
        if (lon >= 180.0) {
            lon -= 360.0;
        }
        return new LatLon(lat, lon);
    }

    public static BlockPos toBlock(double latDeg, double lonDeg, int y) {
        double clampedLat = Math.clamp(latDeg, -90.0, 90.0);
        double normLon = ((lonDeg + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        int x = wrapX((int) Math.round((normLon / 180.0) * X_MAX));
        int z = Math.clamp((int) Math.round((-clampedLat / 90.0) * Z_MAX), Z_MIN, Z_MAX - 1);
        return new BlockPos(x, Math.clamp(y, Y_MIN, Y_MAX), z);
    }

    public static double yForAltitude(double meters) {
        if (meters >= 0.0) {
            double y = SEA_LEVEL + 797.0 * Math.pow(meters / 8_850.0, VERTICAL_GAMMA);
            return Math.clamp(y, SEA_LEVEL, Y_MAX - 16.0);
        }
        double depth = -meters;
        if (depth <= 5_000.0) {
            return SEA_LEVEL - 340.0 * Math.pow(depth / 5_000.0, VERTICAL_GAMMA);
        }
        double trenchY = -277.0 - ((depth - 5_000.0) / 6_000.0) * 183.0;
        return Math.clamp(trenchY, -504.0, -277.0);
    }

    public static int blockYForAltitude(double meters) {
        return (int) Math.round(yForAltitude(meters));
    }

    public static double altitudeMeters(double y) {
        if (y >= SEA_LEVEL) {
            return 8_850.0 * Math.pow(Math.max(0.0, (y - SEA_LEVEL) / 797.0), 1.0 / VERTICAL_GAMMA);
        }
        if (y >= -277.0) {
            return -5_000.0 * Math.pow(Math.max(0.0, (SEA_LEVEL - y) / 340.0), 1.0 / VERTICAL_GAMMA);
        }
        return -(5_000.0 + ((-277.0 - y) / 183.0) * 6_000.0);
    }

    public static void l0ToUnitSphere(int gx, int gz, double[] outXYZ) {
        double lonRad = (((gx + 0.5) / L0_WIDTH) * 2.0 - 1.0) * Math.PI;
        double latRad = (0.5 - ((gz + 0.5) / L0_HEIGHT)) * Math.PI;
        double cosLat = Math.cos(latRad);
        outXYZ[0] = cosLat * Math.cos(lonRad);
        outXYZ[1] = Math.sin(latRad);
        outXYZ[2] = cosLat * Math.sin(lonRad);
    }

    public static double solarDeclinationDeg(int dayOfYear) {
        int doy = Math.floorMod(dayOfYear, CivCalendar.YEAR_DAYS);
        return 23.44 * Math.sin(2.0 * Math.PI * doy / CivCalendar.YEAR_DAYS);
    }

    public static float dayLengthHours(double latDeg, int dayOfYear) {
        double declRad = Math.toRadians(solarDeclinationDeg(dayOfYear));
        double latRad = Math.toRadians(Math.clamp(latDeg, -89.99, 89.99));
        double cosH0 = -Math.tan(latRad) * Math.tan(declRad);
        if (cosH0 <= -1.0) {
            return 24.0f;
        }
        if (cosH0 >= 1.0) {
            return 0.0f;
        }
        double h0 = Math.acos(cosH0);
        return (float) (24.0 * (h0 / Math.PI));
    }

    public static float dayLengthTicks(double latDeg, int dayOfYear) {
        return dayLengthHours(latDeg, dayOfYear) * 1_000.0f;
    }

    public static float solarElevationDeg(BlockPos pos, long gameTime) {
        LatLon ll = latLon(pos.getX(), pos.getZ());
        int doy = (int) Math.floorMod(Math.floorDiv(gameTime, CivCalendar.DAY_TICKS), CivCalendar.YEAR_DAYS);
        double declRad = Math.toRadians(solarDeclinationDeg(doy));
        double latRad = Math.toRadians(ll.latDeg());
        long tickOfDay = Math.floorMod(gameTime, CivCalendar.DAY_TICKS);
        double lonSolarOffsetTicks = (ll.lonDeg() / 360.0) * 24_000.0;
        double localTick = Math.floorMod(Math.round(tickOfDay + lonSolarOffsetTicks), 24_000L);
        double hourAngle = ((localTick - 6_000.0) / 24_000.0) * 2.0 * Math.PI;
        double sinEl = Math.sin(latRad) * Math.sin(declRad) + Math.cos(latRad) * Math.cos(declRad) * Math.cos(hourAngle);
        return (float) Math.toDegrees(Math.asin(Math.clamp(sinEl, -1.0, 1.0)));
    }

    public static float remapClientTimeOfDay(int blockX, int blockZ, long dayTime, float partialTick) {
        LatLon ll = latLon(blockX, blockZ);
        int doy = (int) Math.floorMod(Math.floorDiv(dayTime, CivCalendar.DAY_TICKS), CivCalendar.YEAR_DAYS);
        double dayFrac = dayLengthHours(ll.latDeg(), doy) / 24.0;
        double lonTicks = (ll.lonDeg() / 360.0) * 24_000.0;
        double rawTick = (dayTime + partialTick + lonTicks) % 24_000.0;
        if (rawTick < 0.0) {
            rawTick += 24_000.0;
        }
        if (dayFrac >= 0.999) {
            return 0.0f;
        }
        if (dayFrac <= 0.001) {
            return 0.5f;
        }
        double sunrise = 6_000.0 - dayFrac * 12_000.0;
        double sunset = 6_000.0 + dayFrac * 12_000.0;
        double angle;
        if (rawTick >= sunrise && rawTick <= sunset) {
            double u = (rawTick - sunrise) / Math.max(1.0, sunset - sunrise);
            angle = 0.75 + u * 0.5;
        } else {
            double nightLen = 24_000.0 - (sunset - sunrise);
            double nightElapsed = rawTick > sunset ? (rawTick - sunset) : (24_000.0 - sunset + rawTick);
            double u = nightElapsed / Math.max(1.0, nightLen);
            angle = 0.25 + u * 0.5;
        }
        return (float) (angle - Math.floor(angle));
    }

    public static boolean isInMirrorBuffer(int x) {
        return (x >= X_MIN - WRAP_BUFFER && x < X_MIN) || (x >= X_MAX && x < X_MAX + WRAP_BUFFER);
    }
}
