package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Simulated ancient polity record (18 §5.1.5, §5.1.8, §11.1).
 */
public record Polity(
        int id,
        int cultureId,
        String name,
        int foundedYear,
        int endYear,
        int peakEra,
        int finalEra,
        int finalPopulation,
        long capitalRegion,
        PolityFate fate,
        List<Integer> predecessorIds,
        UUID civId
) {
    public static final Codec<Polity> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Polity::id),
            Codec.INT.fieldOf("culture_id").forGetter(Polity::cultureId),
            Codec.STRING.fieldOf("name").forGetter(Polity::name),
            Codec.INT.fieldOf("founded_year").forGetter(Polity::foundedYear),
            Codec.INT.fieldOf("end_year").forGetter(Polity::endYear),
            Codec.INT.fieldOf("peak_era").forGetter(Polity::peakEra),
            Codec.INT.fieldOf("final_era").forGetter(Polity::finalEra),
            Codec.INT.fieldOf("final_population").forGetter(Polity::finalPopulation),
            Codec.LONG.fieldOf("capital_region").forGetter(Polity::capitalRegion),
            PolityFate.CODEC.fieldOf("fate").forGetter(Polity::fate),
            Codec.INT.listOf().optionalFieldOf("predecessors", List.of()).forGetter(Polity::predecessorIds),
            UUIDUtil.CODEC.fieldOf("civ_id").forGetter(Polity::civId)
    ).apply(i, Polity::new));

    public Polity {
        Objects.requireNonNull(name, "name");
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        Objects.requireNonNull(fate, "fate");
        predecessorIds = predecessorIds == null ? List.of() : List.copyOf(predecessorIds);
        Objects.requireNonNull(civId, "civId");
    }

    public Polity withCivId(UUID updatedCivId) {
        return new Polity(
                id, cultureId, name, foundedYear, endYear, peakEra, finalEra,
                finalPopulation, capitalRegion, fate, predecessorIds, updatedCivId
        );
    }
}
