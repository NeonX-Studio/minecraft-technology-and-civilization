package com.nightcity.tcivilization.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Settlement registration facade publishing core settlement lifecycle events (03 §5.1.3, §12.1).
 */
public interface SettlementRegistry {
    SettlementRecord found(UUID civ, String name, GlobalPos core);

    void setTier(UUID settlement, int tier);

    void setPopulation(UUID settlement, int population);

    void remove(UUID settlement, Identifier reason);

    Optional<SettlementRecord> get(UUID id);

    List<SettlementRecord> inRegion(ResourceKey<Level> dim, long regionKey);

    List<SettlementRecord> all();
}
