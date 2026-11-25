package com.nightcity.tcivilization;

/**
 * Converts overworld block coordinates into the fixed 512-block civilization grid.
 *
 * <p>The X axis wraps so that the east and west edges share the same region space.
 * The Z axis is clamped to the supported overworld bounds.</p>
 */
public record RegionCoordinates(int x, int z) {
    public static final int REGION_SIZE = 512;
    public static final int REGION_WIDTH = 320;
    public static final int REGION_HEIGHT = 160;
    public static final int WORLD_WIDTH = REGION_SIZE * REGION_WIDTH;
    public static final int HALF_WIDTH = WORLD_WIDTH / 2;
    public static final int Z_LIMIT = REGION_SIZE * REGION_HEIGHT / 2;

    public RegionCoordinates {
        if (x < -REGION_WIDTH / 2 || x >= REGION_WIDTH / 2) {
            throw new IllegalArgumentException("Region X is outside the overworld grid: " + x);
        }
        if (z < -REGION_HEIGHT / 2 || z >= REGION_HEIGHT / 2) {
            throw new IllegalArgumentException("Region Z is outside the overworld grid: " + z);
        }
    }

    public static RegionCoordinates fromBlock(int blockX, int blockZ) {
        return new RegionCoordinates(regionX(blockX), regionZ(blockZ));
    }

    public static int wrapX(int blockX) {
        long shifted = (long) blockX + HALF_WIDTH;
        return (int) Math.floorMod(shifted, (long) WORLD_WIDTH) - HALF_WIDTH;
    }

    public static int regionX(int blockX) {
        return Math.floorDiv(wrapX(blockX), REGION_SIZE);
    }

    public static int regionZ(int blockZ) {
        int clamped = Math.max(-Z_LIMIT, Math.min(Z_LIMIT - 1, blockZ));
        return Math.floorDiv(clamped, REGION_SIZE);
    }

    public long key() {
        return key(x, z);
    }

    public static long key(int regionX, int regionZ) {
        validateRegionX(regionX);
        validateRegionZ(regionZ);
        return ((long) regionX << 32) | (regionZ & 0xffff_ffffL);
    }

    public int fileX() {
        return Math.floorDiv(x, 8);
    }

    public int fileZ() {
        return Math.floorDiv(z, 8);
    }

    public int centerBlockX() {
        return x * REGION_SIZE + REGION_SIZE / 2;
    }

    public int centerBlockZ() {
        return z * REGION_SIZE + REGION_SIZE / 2;
    }

    private static void validateRegionX(int regionX) {
        if (regionX < -REGION_WIDTH / 2 || regionX >= REGION_WIDTH / 2) {
            throw new IllegalArgumentException("Region X is outside the overworld grid: " + regionX);
        }
    }

    private static void validateRegionZ(int regionZ) {
        if (regionZ < -REGION_HEIGHT / 2 || regionZ >= REGION_HEIGHT / 2) {
            throw new IllegalArgumentException("Region Z is outside the overworld grid: " + regionZ);
        }
    }
}
