package com.nightcity.tcivilization.api;

/** Describes the region dimensions and coordinate policy for one dimension. */
public record RegionGridSpec(String dimension, int width, int height, boolean wrapX) {
    public static final int REGION_SIZE = 512;

    public RegionGridSpec {
        if (dimension == null) {
            throw new IllegalArgumentException("Dimension must be present");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive");
        }
    }

    public static RegionGridSpec overworld() {
        return new RegionGridSpec("minecraft:overworld", 320, 160, true);
    }

    public static RegionGridSpec nether() {
        return new RegionGridSpec("minecraft:the_nether", 320, 160, false);
    }

    public static RegionGridSpec end() {
        return new RegionGridSpec("minecraft:the_end", 64, 64, false);
    }

    public RegionCell atBlock(int blockX, int blockZ) {
        int normalizedX = normalizeBlockX(blockX);
        int normalizedZ = clampBlockZ(blockZ);
        return new RegionCell(dimension, Math.floorDiv(normalizedX, REGION_SIZE),
                Math.floorDiv(normalizedZ, REGION_SIZE));
    }

    public int normalizeBlockX(int blockX) {
        int limit = width * REGION_SIZE / 2;
        if (wrapX) {
            long shifted = (long) blockX + limit;
            return (int) Math.floorMod(shifted, (long) width * REGION_SIZE) - limit;
        }
        return Math.max(-limit, Math.min(limit - 1, blockX));
    }

    public int clampBlockZ(int blockZ) {
        int limit = height * REGION_SIZE / 2;
        return Math.max(-limit, Math.min(limit - 1, blockZ));
    }

    public boolean contains(int regionX, int regionZ) {
        return regionX >= -width / 2 && regionX < width - width / 2
                && regionZ >= -height / 2 && regionZ < height - height / 2;
    }
}
