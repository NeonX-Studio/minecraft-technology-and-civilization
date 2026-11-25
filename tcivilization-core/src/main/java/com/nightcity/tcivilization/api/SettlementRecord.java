package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

/**
 * Core settlement registry record (03 §5.1.3, §11).
 */
public record SettlementRecord(
        UUID id,
        UUID civ,
        String name,
        GlobalPos corePos,
        long regionKey,
        int tier,
        int population,
        long foundedTick
) {
    public static final Codec<SettlementRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(SettlementRecord::id),
            UUIDUtil.CODEC.fieldOf("civ").forGetter(SettlementRecord::civ),
            Codec.STRING.fieldOf("name").forGetter(SettlementRecord::name),
            GlobalPos.CODEC.fieldOf("core_pos").forGetter(SettlementRecord::corePos),
            Codec.LONG.fieldOf("region_key").forGetter(SettlementRecord::regionKey),
            Codec.intRange(0, 6).fieldOf("tier").forGetter(SettlementRecord::tier),
            Codec.INT.fieldOf("population").forGetter(SettlementRecord::population),
            Codec.LONG.fieldOf("founded_tick").forGetter(SettlementRecord::foundedTick)
    ).apply(i, SettlementRecord::new));

    public SettlementRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(civ, "civ");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(corePos, "corePos");
        tier = Math.clamp(tier, 0, 6);
        population = Math.max(0, population);
    }

    public SettlementRecord withTier(int nextTier) {
        return new SettlementRecord(id, civ, name, corePos, regionKey, Math.clamp(nextTier, 0, 6), population, foundedTick);
    }

    public SettlementRecord withPopulation(int nextPopulation) {
        return new SettlementRecord(id, civ, name, corePos, regionKey, tier, Math.max(0, nextPopulation), foundedTick);
    }
}
