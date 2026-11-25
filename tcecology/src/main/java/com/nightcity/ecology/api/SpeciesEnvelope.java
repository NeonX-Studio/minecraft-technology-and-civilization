package com.nightcity.ecology.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;

/**
 * Data-driven climate & habitat suitability envelope for a wildlife species (11 §5.2, §5.3, §7.1).
 */
public record SpeciesEnvelope(
        Identifier id,
        Identifier entityId,
        Identifier fallbackVanillaEntityId,
        MobCategory mobCategory,
        int kBase,
        int minGroup,
        int maxGroup,
        double tempOptC,
        double tempWidthC,
        double precipOptMm,
        double precipWidthMm,
        Map<HabitatCategory, Double> habitatPrefs,
        boolean relict
) {
    public static final double MIN_SUITABILITY_THRESHOLD = 0.18D;

    private static final Codec<MobCategory> MOB_CATEGORY_CODEC = Codec.STRING.xmap(
            SpeciesEnvelope::parseCategory,
            c -> c.getName().toLowerCase(Locale.ROOT)
    );

    public static final Codec<SpeciesEnvelope> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(SpeciesEnvelope::id),
            Identifier.CODEC.fieldOf("entity").forGetter(SpeciesEnvelope::entityId),
            Identifier.CODEC.fieldOf("fallback_entity").forGetter(SpeciesEnvelope::fallbackVanillaEntityId),
            MOB_CATEGORY_CODEC.fieldOf("category").forGetter(SpeciesEnvelope::mobCategory),
            Codec.INT.fieldOf("k_base").forGetter(SpeciesEnvelope::kBase),
            Codec.INT.fieldOf("min_group").forGetter(SpeciesEnvelope::minGroup),
            Codec.INT.fieldOf("max_group").forGetter(SpeciesEnvelope::maxGroup),
            Codec.DOUBLE.fieldOf("t_opt").forGetter(SpeciesEnvelope::tempOptC),
            Codec.DOUBLE.fieldOf("t_width").forGetter(SpeciesEnvelope::tempWidthC),
            Codec.DOUBLE.fieldOf("p_opt").forGetter(SpeciesEnvelope::precipOptMm),
            Codec.DOUBLE.fieldOf("p_width").forGetter(SpeciesEnvelope::precipWidthMm),
            Codec.unboundedMap(HabitatCategory.CODEC, Codec.DOUBLE).fieldOf("habitat").forGetter(SpeciesEnvelope::habitatPrefs),
            Codec.BOOL.optionalFieldOf("relict", false).forGetter(SpeciesEnvelope::relict)
    ).apply(instance, SpeciesEnvelope::new));

    public SpeciesEnvelope {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(fallbackVanillaEntityId, "fallbackVanillaEntityId");
        Objects.requireNonNull(mobCategory, "mobCategory");
        if (mobCategory == MobCategory.MONSTER) {
            throw new IllegalArgumentException("Ecology SpeciesEnvelope must never target MobCategory.MONSTER: " + id);
        }
        kBase = Math.max(1, kBase);
        minGroup = Math.max(1, minGroup);
        maxGroup = Math.max(minGroup, maxGroup);
        tempWidthC = Math.max(1.0D, tempWidthC);
        precipWidthMm = Math.max(50.0D, precipWidthMm);
        EnumMap<HabitatCategory, Double> copy = new EnumMap<>(HabitatCategory.class);
        if (habitatPrefs != null) {
            for (Map.Entry<HabitatCategory, Double> e : habitatPrefs.entrySet()) {
                copy.put(e.getKey(), Math.max(0.0D, Math.min(1.0D, e.getValue())));
            }
        }
        habitatPrefs = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(copy));
    }

    /**
     * Computes habitat quality HQ_s = clamp(H_s * C_s, 0, 1) per 11 §5.3.
     */
    public double suitability(double tempC, double precipMm, Map<HabitatCategory, Double> landCover) {
        double hMatch = 0.0D;
        for (Map.Entry<HabitatCategory, Double> pref : habitatPrefs.entrySet()) {
            double cover = landCover.getOrDefault(pref.getKey(), 0.0D);
            hMatch += pref.getValue() * cover;
        }
        if (hMatch <= 0.001D) {
            return 0.0D;
        }
        double dt = (tempC - tempOptC) / tempWidthC;
        double dp = (precipMm - precipOptMm) / precipWidthMm;
        double climate = StrictMath.exp(-(dt * dt)) * StrictMath.exp(-(dp * dp));
        return Math.max(0.0D, Math.min(1.0D, hMatch * climate));
    }

    public int spawnWeight(double suitabilityScore) {
        if (suitabilityScore < MIN_SUITABILITY_THRESHOLD) {
            return 0;
        }
        return Math.max(1, (int) StrictMath.rint(kBase * 0.25D * suitabilityScore));
    }

    private static MobCategory parseCategory(String raw) {
        String normalized = raw.toUpperCase(Locale.ROOT);
        for (MobCategory cat : MobCategory.values()) {
            if (cat.name().equals(normalized) || cat.getName().equalsIgnoreCase(raw)) {
                return cat;
            }
        }
        throw new IllegalArgumentException("Unknown MobCategory: " + raw);
    }
}
