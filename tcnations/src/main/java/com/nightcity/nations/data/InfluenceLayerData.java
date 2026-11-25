package com.nightcity.nations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Serializable data for region layer tcnations:influence (23 §5.2, §11.1).
 * Stores the top-3 civilization influence values (0..1000) and computes deterministic ownership.
 */
public record InfluenceLayerData(
        int formatVersion,
        List<Entry> topEntries,
        boolean breathingZoneRestricted
) {
    public static final int FORMAT_VERSION = 1;
    public static final short MAX_INFLUENCE = 1000;
    public static final short BREATHING_ZONE_AI_CAP = 199;
    public static final short OWNERSHIP_THRESHOLD = 200;
    public static final short OWNERSHIP_LEAD_MARGIN = 50;

    public record Entry(UUID civId, byte civIndex, short influence) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("civ_id").forGetter(Entry::civId),
                Codec.BYTE.optionalFieldOf("idx", (byte) 0).forGetter(Entry::civIndex),
                Codec.SHORT.fieldOf("val").forGetter(Entry::influence)
        ).apply(instance, Entry::new));

        public Entry {
            Objects.requireNonNull(civId, "civId");
            influence = (short) Math.max(0, Math.min(MAX_INFLUENCE, influence));
        }
    }

    public static final Codec<InfluenceLayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("format_version", FORMAT_VERSION).forGetter(InfluenceLayerData::formatVersion),
            Entry.CODEC.listOf().optionalFieldOf("top", List.of()).forGetter(InfluenceLayerData::topEntries),
            Codec.BOOL.optionalFieldOf("breath_restricted", false).forGetter(InfluenceLayerData::breathingZoneRestricted)
    ).apply(instance, InfluenceLayerData::new));

    public InfluenceLayerData {
        List<Entry> sorted = new ArrayList<>(topEntries == null ? List.of() : topEntries);
        sorted.removeIf(e -> e == null || e.influence() <= 0);
        sorted.sort(Comparator.<Entry>comparingInt(Entry::influence).reversed()
                .thenComparingInt(Entry::civIndex)
                .thenComparing(Entry::civId));
        if (sorted.size() > 3) {
            sorted = sorted.subList(0, 3);
        }
        topEntries = List.copyOf(sorted);
    }

    public static InfluenceLayerData empty() {
        return new InfluenceLayerData(FORMAT_VERSION, List.of(), false);
    }

    public static InfluenceLayerData fromScores(
            Map<UUID, Integer> rawInfluences,
            Map<UUID, Byte> indexByCiv,
            UUID playerCivId,
            boolean insidePlayerBreathingZone
    ) {
        Map<UUID, Entry> merged = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : rawInfluences.entrySet()) {
            UUID civId = entry.getKey();
            int val = Math.max(0, Math.min(MAX_INFLUENCE, entry.getValue()));
            if (insidePlayerBreathingZone && !Objects.equals(civId, playerCivId)) {
                val = Math.min(BREATHING_ZONE_AI_CAP, val);
            }
            if (val > 0) {
                byte idx = indexByCiv.getOrDefault(civId, (byte) 0);
                merged.put(civId, new Entry(civId, idx, (short) val));
            }
        }
        return new InfluenceLayerData(FORMAT_VERSION, new ArrayList<>(merged.values()), insidePlayerBreathingZone);
    }

    public int influenceOf(UUID civId) {
        for (Entry e : topEntries) {
            if (e.civId().equals(civId)) {
                return e.influence();
            }
        }
        return 0;
    }

    /**
     * Ownership rule (23 §5.2): I_top >= 200 and (I_top - I_second) >= 50.
     */
    public Optional<UUID> ownerCivId() {
        if (topEntries.isEmpty()) {
            return Optional.empty();
        }
        Entry top = topEntries.getFirst();
        if (top.influence() < OWNERSHIP_THRESHOLD) {
            return Optional.empty();
        }
        int second = topEntries.size() >= 2 ? topEntries.get(1).influence() : 0;
        if (top.influence() - second < OWNERSHIP_LEAD_MARGIN) {
            return Optional.empty();
        }
        return Optional.of(top.civId());
    }
}
