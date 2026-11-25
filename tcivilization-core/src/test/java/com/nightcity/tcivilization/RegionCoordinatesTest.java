package com.nightcity.tcivilization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionCoordinatesTest {
    @Test
    void wrapsTheEastAndWestEdgesIntoAdjacentRegions() {
        assertEquals(-RegionCoordinates.HALF_WIDTH, RegionCoordinates.wrapX(RegionCoordinates.HALF_WIDTH));
        assertEquals(RegionCoordinates.HALF_WIDTH - 1,
                RegionCoordinates.wrapX(-RegionCoordinates.HALF_WIDTH - 1));
        assertEquals(-160, RegionCoordinates.regionX(RegionCoordinates.HALF_WIDTH));
        assertEquals(159, RegionCoordinates.regionX(-RegionCoordinates.HALF_WIDTH - 1));
    }

    @Test
    void mapsNegativeBlockCoordinatesWithFloorDivision() {
        assertEquals(new RegionCoordinates(-1, -1), RegionCoordinates.fromBlock(-1, -1));
        assertEquals(new RegionCoordinates(-160, -80),
                RegionCoordinates.fromBlock(-RegionCoordinates.HALF_WIDTH, -RegionCoordinates.Z_LIMIT));
        assertEquals(new RegionCoordinates(159, 79),
                RegionCoordinates.fromBlock(RegionCoordinates.HALF_WIDTH - 1, RegionCoordinates.Z_LIMIT - 1));
    }

    @Test
    void clampsZOutsideTheSupportedGrid() {
        assertEquals(-80, RegionCoordinates.regionZ(Integer.MIN_VALUE));
        assertEquals(79, RegionCoordinates.regionZ(Integer.MAX_VALUE));
    }

    @Test
    void exposesStorageKeyFileCoordinatesAndCenter() {
        RegionCoordinates region = new RegionCoordinates(-9, 17);

        assertEquals(((long) -9 << 32) | 17L, region.key());
        assertEquals(-2, region.fileX());
        assertEquals(2, region.fileZ());
        assertEquals(-4352, region.centerBlockX());
        assertEquals(8960, region.centerBlockZ());
    }

    @Test
    void rejectsRegionsOutsideTheOverworldGrid() {
        assertThrows(IllegalArgumentException.class, () -> new RegionCoordinates(-161, 0));
        assertThrows(IllegalArgumentException.class, () -> new RegionCoordinates(0, 80));
        assertThrows(IllegalArgumentException.class, () -> RegionCoordinates.key(160, 0));
    }
}
