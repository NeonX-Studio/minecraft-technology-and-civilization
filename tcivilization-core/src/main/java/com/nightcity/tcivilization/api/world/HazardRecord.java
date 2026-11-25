package com.nightcity.tcivilization.api.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Historical or abstract region hazard log entry (10 §11.4, 03 §12.5).
 */
public record HazardRecord(
        long id,
        HazardType type,
        long startTick,
        long endTick,
        float magnitude,
        boolean abstractOnly,
        float cropLossFraction,
        float buildingDamageIndex,
        int casualtiesEstimate
) {
    public static final Codec<HazardRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("id").forGetter(HazardRecord::id),
            HazardType.CODEC.fieldOf("type").forGetter(HazardRecord::type),
            Codec.LONG.fieldOf("start").forGetter(HazardRecord::startTick),
            Codec.LONG.fieldOf("end").forGetter(HazardRecord::endTick),
            Codec.FLOAT.fieldOf("mag").forGetter(HazardRecord::magnitude),
            Codec.BOOL.optionalFieldOf("abstract", false).forGetter(HazardRecord::abstractOnly),
            Codec.FLOAT.optionalFieldOf("crop_loss", 0.0f).forGetter(HazardRecord::cropLossFraction),
            Codec.FLOAT.optionalFieldOf("bldg_dmg", 0.0f).forGetter(HazardRecord::buildingDamageIndex),
            Codec.INT.optionalFieldOf("casualties", 0).forGetter(HazardRecord::casualtiesEstimate)
    ).apply(instance, HazardRecord::new));
}
