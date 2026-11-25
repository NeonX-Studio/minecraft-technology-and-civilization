package com.nightcity.tcivilization.region;

import com.mojang.serialization.Codec;
import com.nightcity.tcivilization.api.LayerSync;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionGridRegistry;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.RegionLayer;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class TcrRegionFileTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsMultipleCellsAndPreservesUnregisteredLayers() throws IOException {
        Path file = tempDir.resolve("r.0.0.tcr");
        String civKey = CivRegionLayer.ID.toString();
        String unknownModLayer = "tcecology:pollution";

        CompoundTag civTag = new CompoundTag();
        civTag.putInt("person_count", 42);
        CompoundTag ecoTag = new CompoundTag();
        ecoTag.putDouble("soot_kg", 128.5D);

        Map<Integer, TcrRegionFile.RegionBlob> payload = new LinkedHashMap<>();
        payload.put(TcrRegionFile.slotIndex(0, 0), new TcrRegionFile.RegionBlob(1, Map.of(civKey, civTag, unknownModLayer, ecoTag)));
        payload.put(TcrRegionFile.slotIndex(7, 7), new TcrRegionFile.RegionBlob(1, Map.of(civKey, civTag)));

        TcrRegionFile.writeRegions(file, 4082, payload, 100L, (p, r) -> {});
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(tempDir.resolve("r.0.0.tcr.tmp")));

        TcrRegionFile.RegionBlob loaded = TcrRegionFile.readRegion(
                file, new RegionCell("minecraft:overworld", 0, 0), 100L, (p, r) -> {});
        assertEquals(2, loaded.layers().size());
        assertEquals(42, ((CompoundTag) loaded.layers().get(civKey)).getIntOr("person_count", 0));
        assertEquals(128.5D, ((CompoundTag) loaded.layers().get(unknownModLayer)).getDoubleOr("soot_kg", 0.0D));
    }

    @Test
    void rejectsLayerExceedingSixteenKilobytes() {
        Path file = tempDir.resolve("r.oversized.tcr");
        CompoundTag huge = new CompoundTag();
        huge.putByteArray("blob", new byte[RegionLayer.MAX_SERIALIZED_BYTES + 128]);

        Map<Integer, TcrRegionFile.RegionBlob> payload = Map.of(
                0, new TcrRegionFile.RegionBlob(1, Map.of("test:huge", huge))
        );
        assertThrows(IllegalArgumentException.class, () -> TcrRegionFile.writeRegions(file, 1, payload, 1L, (p, r) -> {}));
    }

    @Test
    void quarantinesCorruptTcrFileWithTickSuffixAndReturnsEmpty() throws IOException {
        Path file = tempDir.resolve("r.-1.2.tcr");
        CompoundTag tag = new CompoundTag();
        tag.putString("hello", "world");
        TcrRegionFile.writeRegions(file, 1, Map.of(TcrRegionFile.slotIndex(3, 0), new TcrRegionFile.RegionBlob(1, Map.of(CivRegionLayer.ID.toString(), tag))), 1L, (p, r) -> {});

        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] ^= 0x5A;
        Files.write(file, bytes);

        TcrRegionFile.RegionBlob recovered = TcrRegionFile.readRegion(
                file, new RegionCell("minecraft:overworld", 3, 0), 9876L, (p, r) -> {});
        assertTrue(recovered.layers().isEmpty());
        assertFalse(Files.exists(file));
        assertTrue(Files.exists(tempDir.resolve("r.-1.2.tcr.corrupt.9876")));
    }

    @Test
    void regionManagerHandlesCacheEvictionPinningAndOverworldWrapSeam() {
        com.nightcity.tcivilization.api.TcThreads.bindMainThread(Thread.currentThread());
        try {
            RegionLayerRegistry registry = new RegionLayerRegistry();
            registry.register(CivRegionLayer.INSTANCE);
            Identifier customId = Identifier.fromNamespaceAndPath("testmod", "fertility");
            RegionLayer<Integer> fertilityLayer = new RegionLayer<>() {
                @Override
                public Identifier id() {
                    return customId;
                }

                @Override
                public Codec<Integer> codec() {
                    return Codec.INT;
                }

                @Override
                public Integer createDefault(RegionCell cell) {
                    return 50;
                }
            };
            registry.register(fertilityLayer);

            TcEventBus bus = new TcEventBus(e -> {});
            Object pinOwner = new Object();
            try (RegionManager manager = new RegionManager(
                    tempDir, 1, 2, RegionGridRegistry.defaults(), registry, bus, () -> 240L
            )) {
                RegionHandle eastEdge = manager.atBlock("minecraft:overworld", 81_919, 0).orElseThrow();
                RegionHandle wrappedWest = manager.atBlock("minecraft:overworld", 81_920, 0).orElseThrow();
                assertEquals(159, eastEdge.cell().x());
                assertEquals(-160, wrappedWest.cell().x());

                UUID settlementId = UUID.randomUUID();
                eastEdge.update(CivRegionLayer.INSTANCE, d -> d.withPersonCounts(12, 0, 0).withSettlementAdded(settlementId));
                eastEdge.update(fertilityLayer, v -> 88);
                manager.pin(eastEdge, pinOwner);

                manager.getByDimensionId("minecraft:overworld", 0, 0).orElseThrow();
                manager.getByDimensionId("minecraft:overworld", 1, 0).orElseThrow();
                assertTrue(manager.isCached(eastEdge.cell()));
                assertEquals(88, eastEdge.layer(fertilityLayer));

                manager.unpin(eastEdge, pinOwner);
                manager.flushAllSync();
            }

            try (RegionManager reopened = new RegionManager(
                    tempDir, 1, 8, RegionGridRegistry.defaults(), registry, bus, () -> 500L
            )) {
                RegionHandle reloaded = reopened.atBlock("minecraft:overworld", 81_919, 0).orElseThrow();
                assertEquals(12, reloaded.layer(CivRegionLayer.INSTANCE).personCount());
                assertEquals(1, reloaded.layer(CivRegionLayer.INSTANCE).settlementIds().size());
                assertEquals(88, reloaded.layer(fertilityLayer));
            }
        } finally {
            com.nightcity.tcivilization.api.TcThreads.bindMainThread(null);
        }
    }
}
