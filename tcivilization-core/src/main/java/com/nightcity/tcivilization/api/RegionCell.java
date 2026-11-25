package com.nightcity.tcivilization.api;

/** Identifies one region cell within a registered dimension grid. */
public record RegionCell(String dimension, int x, int z) {
    public long key() {
        return ((long) x << 32) | (z & 0xffff_ffffL);
    }

    public int fileX() {
        return Math.floorDiv(x, 8);
    }

    public int fileZ() {
        return Math.floorDiv(z, 8);
    }
}
