package com.nightcity.technology.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record EraDefinition(
        Identifier id,
        int index,
        List<Identifier> keystones,
        Identifier milestone,
        int populationMin,
        int fallbackPopulationMin,
        List<Identifier> newVerbs,
        Identifier music,
        String hudPalette
) {
    public static final Codec<EraDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(EraDefinition::id),
            Codec.INT.fieldOf("index").forGetter(EraDefinition::index),
            Identifier.CODEC.listOf().fieldOf("keystones").forGetter(EraDefinition::keystones),
            Identifier.CODEC.fieldOf("milestone").forGetter(EraDefinition::milestone),
            Codec.INT.fieldOf("population_min").forGetter(EraDefinition::populationMin),
            Codec.INT.optionalFieldOf("fallback_population_min", 6).forGetter(EraDefinition::fallbackPopulationMin),
            Identifier.CODEC.listOf().optionalFieldOf("new_verbs", List.of()).forGetter(EraDefinition::newVerbs),
            Identifier.CODEC.fieldOf("music").forGetter(EraDefinition::music),
            Codec.STRING.optionalFieldOf("hud_palette", "tcatlas:skin/stone").forGetter(EraDefinition::hudPalette)
    ).apply(i, EraDefinition::new));

    public record EraProgress(
            TechEra current,
            Optional<TechEra> next,
            int keystonesDone,
            int keystonesTotal,
            boolean milestoneDone,
            int population,
            int populationRequired,
            boolean canDeclare
    ) {
    }
}
