package com.nightcity.wildlands.planet;

import com.nightcity.tcivilization.api.world.PlanetApi;

/**
 * Exact integer-lattice periodic 2D/3D gradient and FBM/ridge noise in X with period PLANET_WIDTH = 163,840 (10 §5.9.2-5.9.3).
 * Because X is normalized to [X_MIN, X_MAX) via integer floorMod before lattice hashing, noise(x, ...) == noise(x + 163840, ...) is bit-exact.
 */
public final class PeriodicNoise {
    public static final int PLANET_WIDTH = PlanetApi.PLANET_WIDTH;
    private static final int X_MIN = PlanetApi.X_MIN;
    private static final double LAT_BAND_BLOCKS = 2_048.0;
    private static final double LAT_BLEND_BLOCKS = 64.0;

    private PeriodicNoise() {
    }

    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    public static long hash2D(long seed, int ix, int iz) {
        long h = seed ^ (ix * 0x9E3779B97F4A7C15L) ^ (iz * 0xC2B2AE3D27D4EB4FL);
        return mix64(h);
    }

    public static long hash3D(long seed, int ix, int iy, int iz) {
        long h = seed ^ (ix * 0x9E3779B97F4A7C15L) ^ (iy * 0xBF58476D1CE4E5B9L) ^ (iz * 0xC2B2AE3D27D4EB4FL);
        return mix64(h);
    }

    private static double quintic(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double grad2D(long hash, double dx, double dz) {
        int g = (int) (hash & 7L);
        return switch (g) {
            case 0 -> dx + dz;
            case 1 -> -dx + dz;
            case 2 -> dx - dz;
            case 3 -> -dx - dz;
            case 4 -> dx * 1.4142135623730951;
            case 5 -> -dx * 1.4142135623730951;
            case 6 -> dz * 1.4142135623730951;
            default -> -dz * 1.4142135623730951;
        };
    }

    private static double grad3D(long hash, double dx, double dy, double dz) {
        int h = (int) (hash & 15L);
        double u = h < 8 ? dx : dy;
        double v = h < 4 ? dy : (h == 12 || h == 14 ? dx : dz);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public static double sample2D(long seed, int x, int z, int nominalSpacing) {
        long wrappedOffsetX = Math.floorMod((long) x - X_MIN, (long) PLANET_WIDTH);
        int periodCells = Math.max(4, Math.round((float) PLANET_WIDTH / Math.max(1, nominalSpacing)));
        double cellPosX = (wrappedOffsetX * (double) periodCells) / PLANET_WIDTH;
        double exactSpacingZ = (double) PLANET_WIDTH / periodCells;
        double cellPosZ = z / exactSpacingZ;
        return evalPeriodicGrid2D(seed, cellPosX, cellPosZ, periodCells);
    }

    public static double sample2DContinuous(long seed, double x, double z, double nominalSpacing) {
        double shifted = x - X_MIN;
        double wrapped = shifted - Math.floor(shifted / PLANET_WIDTH) * PLANET_WIDTH;
        int periodCells = Math.max(4, (int) Math.round(PLANET_WIDTH / Math.max(1.0, nominalSpacing)));
        double cellPosX = (wrapped / PLANET_WIDTH) * periodCells;
        double exactSpacingZ = (double) PLANET_WIDTH / periodCells;
        double cellPosZ = z / exactSpacingZ;
        return evalPeriodicGrid2D(seed, cellPosX, cellPosZ, periodCells);
    }

    private static double evalPeriodicGrid2D(long seed, double cellPosX, double cellPosZ, int periodCells) {
        int ix0 = (int) Math.floor(cellPosX);
        int iz0 = (int) Math.floor(cellPosZ);
        double fx = cellPosX - ix0;
        double fz = cellPosZ - iz0;

        int wx0 = Math.floorMod(ix0, periodCells);
        int wx1 = Math.floorMod(ix0 + 1, periodCells);
        int iz1 = iz0 + 1;

        double n00 = grad2D(hash2D(seed, wx0, iz0), fx, fz);
        double n10 = grad2D(hash2D(seed, wx1, iz0), fx - 1.0, fz);
        double n01 = grad2D(hash2D(seed, wx0, iz1), fx, fz - 1.0);
        double n11 = grad2D(hash2D(seed, wx1, iz1), fx - 1.0, fz - 1.0);

        double ux = quintic(fx);
        double uz = quintic(fz);
        return Math.clamp(lerp(lerp(n00, n10, ux), lerp(n01, n11, ux), uz) * 0.7071067811865475, -1.0, 1.0);
    }

    public static double sample3D(long seed, int x, int y, int z, int nominalSpacingXZ, int spacingY) {
        long wrappedOffsetX = Math.floorMod((long) x - X_MIN, (long) PLANET_WIDTH);
        int periodCells = Math.max(4, Math.round((float) PLANET_WIDTH / Math.max(1, nominalSpacingXZ)));
        double cellPosX = (wrappedOffsetX * (double) periodCells) / PLANET_WIDTH;
        double exactSpacingZ = (double) PLANET_WIDTH / periodCells;
        double cellPosZ = z / exactSpacingZ;
        double cellPosY = y / (double) Math.max(1, spacingY);

        int ix0 = (int) Math.floor(cellPosX);
        int iy0 = (int) Math.floor(cellPosY);
        int iz0 = (int) Math.floor(cellPosZ);
        double fx = cellPosX - ix0;
        double fy = cellPosY - iy0;
        double fz = cellPosZ - iz0;

        int wx0 = Math.floorMod(ix0, periodCells);
        int wx1 = Math.floorMod(ix0 + 1, periodCells);
        int iy1 = iy0 + 1;
        int iz1 = iz0 + 1;

        double n000 = grad3D(hash3D(seed, wx0, iy0, iz0), fx, fy, fz);
        double n100 = grad3D(hash3D(seed, wx1, iy0, iz0), fx - 1.0, fy, fz);
        double n010 = grad3D(hash3D(seed, wx0, iy1, iz0), fx, fy - 1.0, fz);
        double n110 = grad3D(hash3D(seed, wx1, iy1, iz0), fx - 1.0, fy - 1.0, fz);
        double n001 = grad3D(hash3D(seed, wx0, iy0, iz1), fx, fy, fz - 1.0);
        double n101 = grad3D(hash3D(seed, wx1, iy0, iz1), fx - 1.0, fy, fz - 1.0);
        double n011 = grad3D(hash3D(seed, wx0, iy1, iz1), fx, fy - 1.0, fz - 1.0);
        double n111 = grad3D(hash3D(seed, wx1, iy1, iz1), fx - 1.0, fy - 1.0, fz - 1.0);

        double ux = quintic(fx);
        double uy = quintic(fy);
        double uz = quintic(fz);
        double nx00 = lerp(n000, n100, ux);
        double nx10 = lerp(n010, n110, ux);
        double nx01 = lerp(n001, n101, ux);
        double nx11 = lerp(n011, n111, ux);
        return Math.clamp(lerp(lerp(nx00, nx10, uy), lerp(nx01, nx11, uy), uz) * 0.5773502691896258, -1.0, 1.0);
    }

    public static double fbm2D(long seed, int x, int z, int octaves, int baseSpacing, double gain) {
        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        int spacing = Math.max(2, baseSpacing);
        for (int o = 0; o < octaves; o++) {
            sum += amp * sample2D(seed + o * 0x9E3779B97F4A7C15L, x, z, spacing);
            norm += amp;
            amp *= gain;
            spacing = Math.max(1, spacing / 2);
        }
        return norm > 0.0 ? sum / norm : 0.0;
    }

    public static double sphericalStretchedFbm2D(long seed, int x, int z, int octaves, int baseSpacing, double gain) {
        int wrappedOffsetX = (int) Math.floorMod((long) x - X_MIN, (long) PLANET_WIDTH);
        double bandFloat = (z - PlanetApi.Z_MIN) / LAT_BAND_BLOCKS;
        int band0 = (int) Math.floor(bandFloat);
        double inBandBlocks = (bandFloat - band0) * LAT_BAND_BLOCKS;

        double v0 = evalBandFbm(seed, wrappedOffsetX, z, band0, octaves, baseSpacing, gain);
        if (inBandBlocks < LAT_BLEND_BLOCKS) {
            double t = 0.5 + 0.5 * (inBandBlocks / LAT_BLEND_BLOCKS);
            double vPrev = evalBandFbm(seed, wrappedOffsetX, z, band0 - 1, octaves, baseSpacing, gain);
            return lerp(vPrev, v0, t);
        }
        if (inBandBlocks > LAT_BAND_BLOCKS - LAT_BLEND_BLOCKS) {
            double t = 0.5 * ((inBandBlocks - (LAT_BAND_BLOCKS - LAT_BLEND_BLOCKS)) / LAT_BLEND_BLOCKS);
            double vNext = evalBandFbm(seed, wrappedOffsetX, z, band0 + 1, octaves, baseSpacing, gain);
            return lerp(v0, vNext, t);
        }
        return v0;
    }

    public static double sphericalStretchedRidge2D(long seed, int x, int z, int octaves, int baseSpacing) {
        int wrappedOffsetX = (int) Math.floorMod((long) x - X_MIN, (long) PLANET_WIDTH);
        int band = (int) Math.floor((z - PlanetApi.Z_MIN) / LAT_BAND_BLOCKS);
        double bandCenterZ = PlanetApi.Z_MIN + (band + 0.5) * LAT_BAND_BLOCKS;
        double latDeg = Math.clamp(-bandCenterZ / (double) PlanetApi.Z_MAX * 90.0, -80.0, 80.0);
        double cosLat = Math.cos(Math.toRadians(latDeg));

        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        int spacing = Math.max(4, baseSpacing);
        for (int o = 0; o < octaves; o++) {
            int nLat = Math.max(8, (int) Math.round((PLANET_WIDTH * cosLat) / spacing));
            double cellX = (wrappedOffsetX * (double) nLat) / PLANET_WIDTH;
            double cellZ = z / (double) spacing;
            double raw = evalPeriodicGrid2D(seed + 0x517CC1B727220A95L * (o + 1), cellX, cellZ, nLat);
            double ridge = 1.0 - Math.abs(raw);
            sum += amp * (ridge * ridge);
            norm += amp;
            amp *= 0.5;
            spacing = Math.max(2, spacing / 2);
        }
        return norm > 0.0 ? sum / norm : 0.0;
    }

    private static double evalBandFbm(long seed, int wrappedOffsetX, int z, int bandIndex, int octaves, int baseSpacing, double gain) {
        double bandCenterZ = PlanetApi.Z_MIN + (bandIndex + 0.5) * LAT_BAND_BLOCKS;
        double latDeg = Math.clamp(-bandCenterZ / (double) PlanetApi.Z_MAX * 90.0, -80.0, 80.0);
        double cosLat = Math.cos(Math.toRadians(latDeg));

        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        int spacing = Math.max(2, baseSpacing);
        for (int o = 0; o < octaves; o++) {
            int nLat = Math.max(8, (int) Math.round((PLANET_WIDTH * cosLat) / spacing));
            double cellX = (wrappedOffsetX * (double) nLat) / PLANET_WIDTH;
            double cellZ = z / (double) spacing;
            sum += amp * evalPeriodicGrid2D(seed + o * 0x9E3779B97F4A7C15L, cellX, cellZ, nLat);
            norm += amp;
            amp *= gain;
            spacing = Math.max(1, spacing / 2);
        }
        return norm > 0.0 ? sum / norm : 0.0;
    }
}
