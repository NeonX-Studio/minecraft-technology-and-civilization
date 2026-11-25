package com.nightcity.atlas.map;

/**
 * Plate carree projection, 163,840-block east-west wrapping (DA-13), bearings, and L0..L9 tile pyramid math (22 §5.9).
 */
public final class PlanetProjection {
    public static final int REGION_SIZE = 512;
    public static final int REGION_COLS = 320;
    public static final int REGION_ROWS = 160;
    public static final int WORLD_WIDTH = REGION_SIZE * REGION_COLS;   // 163,840
    public static final int HALF_WIDTH = WORLD_WIDTH / 2;              // 81,920
    public static final int WORLD_HEIGHT = REGION_SIZE * REGION_ROWS;  // 81,920
    public static final int Z_LIMIT = WORLD_HEIGHT / 2;                // 40,960

    public static final int TILE_PIXELS = 256;
    public static final int MIN_ZOOM = 0;
    public static final int MAX_ZOOM = 9;
    public static final double METERS_PER_BLOCK = 244.6;
    public static final double POLAR_LAT_THRESHOLD = 85.0;

    private static final String[] CARDINAL_8 = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
    private static final String[] CARDINAL_16 = {
            "北", "北偏东", "东北", "东偏北", "东", "东偏南", "东南", "南偏东",
            "南", "南偏西", "西南", "西偏南", "西", "西偏北", "西北", "北偏西"
    };

    private PlanetProjection() {
    }

    /**
     * Maps any block X coordinate into canonical planet range [-81,920, 81,920).
     */
    public static int wrapBlockX(int blockX) {
        long shifted = (long) blockX + HALF_WIDTH;
        return (int) (Math.floorMod(shifted, (long) WORLD_WIDTH) - HALF_WIDTH);
    }

    /**
     * Maps any continuous X coordinate or delta into [-81,920.0, 81,920.0).
     */
    public static double wrapX(double x) {
        double shifted = (x + HALF_WIDTH) % WORLD_WIDTH;
        if (shifted < 0.0) {
            shifted += WORLD_WIDTH;
        }
        return shifted - HALF_WIDTH;
    }

    /**
     * Shortest signed east-west displacement from x1 to x2 in [-81,920.0, 81,920.0).
     */
    public static double wrappedDx(double fromX, double toX) {
        return wrapX(toX - fromX);
    }

    public static int clampBlockZ(int blockZ) {
        return Math.clamp(blockZ, -Z_LIMIT, Z_LIMIT - 1);
    }

    public static double distance(double x1, double z1, double x2, double z2, boolean wrapEastWest) {
        double dx = wrapEastWest ? wrappedDx(x1, x2) : (x2 - x1);
        double dz = z2 - z1;
        return Math.hypot(dx, dz);
    }

    /**
     * Clockwise azimuth in radians where 0 = North (-Z), pi/2 = East (+X) (22 §5.9).
     */
    public static double bearingRadians(double x1, double z1, double x2, double z2, boolean wrapEastWest) {
        double dx = wrapEastWest ? wrappedDx(x1, x2) : (x2 - x1);
        double dz = z2 - z1;
        double rad = Math.atan2(dx, -dz);
        return rad < 0.0 ? rad + Math.PI * 2.0 : rad;
    }

    /**
     * Clockwise azimuth in degrees [0.0, 360.0) where 0 = North (-Z), 90 = East (+X), 180 = South (+Z), 270 = West (-X).
     */
    public static double bearingDegrees(double x1, double z1, double x2, double z2, boolean wrapEastWest) {
        double deg = Math.toDegrees(bearingRadians(x1, z1, x2, z2, wrapEastWest)) % 360.0;
        return deg < 0.0 ? deg + 360.0 : deg;
    }

    public static double lonDeg(double x, boolean wrapEastWest) {
        double nx = wrapEastWest ? wrapX(x) : x;
        return (nx / HALF_WIDTH) * 180.0;
    }

    public static double latDeg(double z) {
        return (-z / Z_LIMIT) * 90.0;
    }

    public static boolean isPolar(double z) {
        return Math.abs(latDeg(z)) > POLAR_LAT_THRESHOLD;
    }

    public static int blocksPerPixel(int zoom) {
        int z = Math.clamp(zoom, MIN_ZOOM, MAX_ZOOM);
        return 1 << z;
    }

    public static int tileSpanBlocks(int zoom) {
        return TILE_PIXELS * blocksPerPixel(zoom);
    }

    /**
     * Number of wrapped horizontal tile columns at zoom L.
     * For L=0..7, 163,840 is an exact multiple of 256*2^L (640, 320, 160, 80, 40, 20, 10, 5).
     * For L=8..9, ceiling tile count covering the 163,840-block planet strip from -81,920.
     */
    public static int tileColumns(int zoom) {
        int span = tileSpanBlocks(zoom);
        return (WORLD_WIDTH + span - 1) / span;
    }

    public static int tileRows(int zoom) {
        int span = tileSpanBlocks(zoom);
        return (WORLD_HEIGHT + span - 1) / span;
    }

    /**
     * Canonical tile X index. When wrapEastWest is true, origin is anchored at west dateline (-81,920)
     * so L0..L7 wrap seamlessly modulo tileColumns(zoom) with zero seam at x = +-81,920.
     */
    public static int tileIndexX(int blockX, int zoom, boolean wrapEastWest) {
        int span = tileSpanBlocks(zoom);
        if (!wrapEastWest) {
            return Math.floorDiv(blockX, span);
        }
        int wrappedX = wrapBlockX(blockX);
        int rawIdx = Math.floorDiv(wrappedX + HALF_WIDTH, span);
        if (zoom <= 7) {
            return Math.floorMod(rawIdx, tileColumns(zoom));
        }
        return Math.clamp(rawIdx, 0, tileColumns(zoom) - 1);
    }

    public static int wrapTileIndexX(int rawTileX, int zoom, boolean wrapEastWest) {
        if (!wrapEastWest) {
            return rawTileX;
        }
        return Math.floorMod(rawTileX, tileColumns(zoom));
    }

    public static int tileIndexZ(int blockZ, int zoom, boolean clampPolar) {
        int span = tileSpanBlocks(zoom);
        int z = clampPolar ? clampBlockZ(blockZ) : blockZ;
        if (clampPolar) {
            return Math.floorDiv(z + Z_LIMIT, span);
        }
        return Math.floorDiv(z, span);
    }

    public static int tileOriginBlockX(int tileX, int zoom, boolean wrapEastWest) {
        int span = tileSpanBlocks(zoom);
        if (!wrapEastWest) {
            return tileX * span;
        }
        int wrappedTile = wrapTileIndexX(tileX, zoom, true);
        return -HALF_WIDTH + wrappedTile * span;
    }

    public static int tileOriginBlockZ(int tileZ, int zoom, boolean anchoredAtNorthLimit) {
        int span = tileSpanBlocks(zoom);
        return anchoredAtNorthLimit ? (-Z_LIMIT + tileZ * span) : (tileZ * span);
    }

    public static int pixelToBlockX(int tileX, int px, int zoom, boolean wrapEastWest) {
        int bpp = blocksPerPixel(zoom);
        int raw = tileOriginBlockX(tileX, zoom, wrapEastWest) + px * bpp;
        return wrapEastWest ? wrapBlockX(raw) : raw;
    }

    public static int pixelToBlockZ(int tileZ, int pz, int zoom, boolean anchoredAtNorthLimit) {
        int bpp = blocksPerPixel(zoom);
        return tileOriginBlockZ(tileZ, zoom, anchoredAtNorthLimit) + pz * bpp;
    }

    public static String cardinal8(double bearingDeg) {
        int idx = Math.floorMod((int) Math.round(bearingDeg / 45.0), 8);
        return CARDINAL_8[idx];
    }

    public static String cardinal16(double bearingDeg) {
        int idx = Math.floorMod((int) Math.round(bearingDeg / 22.5), 16);
        return CARDINAL_16[idx];
    }

    public static String formatE1TravelDistance(double blocks) {
        if (blocks <= 215.0) {
            return "就在附近";
        }
        if (blocks <= 1290.0) {
            long hours = Math.max(1L, Math.round(blocks / 215.0));
            return "约 " + hours + " 小时路程";
        }
        if (blocks <= 2600.0) {
            return "约半日路程";
        }
        long days = Math.max(1L, Math.round(blocks / 5200.0));
        return "约 " + days + " 日路程";
    }
}
