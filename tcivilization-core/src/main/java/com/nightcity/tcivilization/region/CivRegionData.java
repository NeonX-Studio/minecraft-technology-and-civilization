package com.nightcity.tcivilization.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Core region layer payload for tcivilization:civ (03 §5.2.3).
 */
public record CivRegionData(
        BitSet discoveredBy,
        long firstVisitTick,
        List<UUID> settlementIds,
        short embodiedCount,
        short proxyCount,
        short abstractCount,
        float pressure,
        short memoryHeat,
        long placedBlocks
) {
    private static final Codec<BitSet> BIT_SET_CODEC = Codec.BYTE_BUFFER.xmap(
            buf -> {
                ByteBuffer slice = buf.slice();
                byte[] bytes = new byte[slice.remaining()];
                slice.get(bytes);
                return BitSet.valueOf(bytes);
            },
            bits -> ByteBuffer.wrap(bits.toByteArray())
    );

    public static final Codec<CivRegionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BIT_SET_CODEC.optionalFieldOf("discovered_by", new BitSet()).forGetter(d -> (BitSet) d.discoveredBy.clone()),
            Codec.LONG.optionalFieldOf("first_visit_tick", -1L).forGetter(CivRegionData::firstVisitTick),
            UUIDUtil.CODEC.listOf().optionalFieldOf("settlements", List.of()).forGetter(CivRegionData::settlementIds),
            Codec.SHORT.optionalFieldOf("embodied", (short) 0).forGetter(CivRegionData::embodiedCount),
            Codec.SHORT.optionalFieldOf("proxy", (short) 0).forGetter(CivRegionData::proxyCount),
            Codec.SHORT.optionalFieldOf("abstract", (short) 0).forGetter(CivRegionData::abstractCount),
            Codec.FLOAT.optionalFieldOf("pressure", 0.0f).forGetter(CivRegionData::pressure),
            Codec.SHORT.optionalFieldOf("memory_heat", (short) 0).forGetter(CivRegionData::memoryHeat),
            Codec.LONG.optionalFieldOf("placed_blocks", 0L).forGetter(CivRegionData::placedBlocks)
    ).apply(instance, CivRegionData::new));

    public CivRegionData {
        discoveredBy = discoveredBy == null ? new BitSet() : (BitSet) discoveredBy.clone();
        settlementIds = settlementIds == null ? List.of() : List.copyOf(settlementIds);
        pressure = Math.max(0.0f, Math.min(1.0f, pressure));
    }

    public static CivRegionData empty() {
        return new CivRegionData(new BitSet(), -1L, List.of(), (short) 0, (short) 0, (short) 0, 0.0f, (short) 0, 0L);
    }

    public short personCount() {
        int sum = Math.max(0, embodiedCount) + Math.max(0, proxyCount) + Math.max(0, abstractCount);
        return (short) Math.min(Short.MAX_VALUE, sum);
    }

    public boolean isDiscoveredBy(int civIndex) {
        return civIndex >= 0 && discoveredBy.get(civIndex);
    }

    public CivRegionData withDiscovered(int civIndex, long tick) {
        BitSet nextBits = (BitSet) discoveredBy.clone();
        if (civIndex >= 0) {
            nextBits.set(civIndex);
        }
        long nextFirst = (civIndex == 0 && firstVisitTick < 0L) ? tick : firstVisitTick;
        return new CivRegionData(nextBits, nextFirst, settlementIds, embodiedCount, proxyCount, abstractCount, pressure, memoryHeat, placedBlocks);
    }

    public CivRegionData withPressure(float nextPressure) {
        return new CivRegionData(discoveredBy, firstVisitTick, settlementIds, embodiedCount, proxyCount, abstractCount, nextPressure, memoryHeat, placedBlocks);
    }

    public CivRegionData withPersonCounts(int embodied, int proxy, int abstracted) {
        return new CivRegionData(
                discoveredBy, firstVisitTick, settlementIds,
                (short) Math.clamp(embodied, 0, Short.MAX_VALUE),
                (short) Math.clamp(proxy, 0, Short.MAX_VALUE),
                (short) Math.clamp(abstracted, 0, Short.MAX_VALUE),
                pressure, memoryHeat, placedBlocks
        );
    }

    public CivRegionData withSettlementAdded(UUID settlementId) {
        if (settlementIds.contains(settlementId)) {
            return this;
        }
        List<UUID> copy = new ArrayList<>(settlementIds);
        copy.add(settlementId);
        return new CivRegionData(discoveredBy, firstVisitTick, copy, embodiedCount, proxyCount, abstractCount, pressure, memoryHeat, placedBlocks);
    }

    public CivRegionData withPlacedBlocks(long nextPlacedBlocks) {
        return new CivRegionData(discoveredBy, firstVisitTick, settlementIds, embodiedCount, proxyCount, abstractCount, pressure, memoryHeat, Math.max(0L, nextPlacedBlocks));
    }

    public CivRegionData withMemoryHeat(int nextHeat) {
        return new CivRegionData(discoveredBy, firstVisitTick, settlementIds, embodiedCount, proxyCount, abstractCount, pressure, (short) Math.clamp(nextHeat, 0, Short.MAX_VALUE), placedBlocks);
    }
}
