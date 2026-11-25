package com.nightcity.tcivilization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegionGridSpecTest {
    private static final String UNKNOWN = "tcivilization:unknown";

    @Test
    void defaultsDescribeSupportedDimensions() {
        RegionGridRegistry registry = RegionGridRegistry.defaults();

        assertEquals(320, registry.get("minecraft:overworld").orElseThrow().width());
        assertTrue(registry.get("minecraft:overworld").orElseThrow().wrapX());
        assertFalse(registry.get("minecraft:the_nether").orElseThrow().wrapX());
        assertEquals(64, registry.get("minecraft:the_end").orElseThrow().width());
        assertTrue(registry.get(UNKNOWN).isEmpty());
        assertTrue(registry.atBlock(UNKNOWN, 0, 0).isEmpty());
    }

    @Test
    void overworldWrapsAcrossTheHorizontalBoundary() {
        RegionGridSpec spec = RegionGridSpec.overworld();

        assertEquals(new RegionCell(spec.dimension(), -160, 0), spec.atBlock(81_920, 0));
        assertEquals(new RegionCell(spec.dimension(), 159, 0), spec.atBlock(-81_921, 0));
    }

    @Test
    void nonWrappingDimensionsClampBothAxes() {
        RegionGridSpec spec = RegionGridSpec.end();

        assertEquals(new RegionCell(spec.dimension(), -32, -32), spec.atBlock(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(new RegionCell(spec.dimension(), 31, 31), spec.atBlock(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void regionCellUsesFloorDivisionForStorageFiles() {
        RegionCell cell = new RegionCell(UNKNOWN, -1, -9);

        assertEquals(-1, cell.fileX());
        assertEquals(-2, cell.fileZ());
        assertEquals(((long) -1 << 32) | 0xffff_fff7L, cell.key());
    }

    @Test
    void invalidSpecsAndDuplicateRegistrationsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RegionGridSpec(UNKNOWN, 0, 1, false));

        RegionGridRegistry registry = new RegionGridRegistry();
        RegionGridSpec spec = new RegionGridSpec(UNKNOWN, 1, 1, false);
        registry.register(spec);
        assertThrows(IllegalArgumentException.class, () -> registry.register(spec));
    }
}
