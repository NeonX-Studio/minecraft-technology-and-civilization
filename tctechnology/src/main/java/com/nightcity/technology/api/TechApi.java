package com.nightcity.technology.api;

import com.nightcity.technology.TTechnology;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TechApi {
    UUID PLAYER_CIV_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    static TechApi get() {
        return TTechnology.api();
    }

    Optional<TechDefinition> definition(Identifier tech);

    Collection<TechDefinition> all();

    TechState state(UUID civ, Identifier tech);

    boolean isUnlocked(UUID civ, Identifier tech);

    boolean isRecipeUnlocked(UUID civ, Identifier recipe);

    default boolean isItemUnlocked(UUID civ, Item item) {
        return isItemUnlocked(civ, BuiltInRegistries.ITEM.getKey(item));
    }

    boolean isItemUnlocked(UUID civ, Identifier itemId);

    Optional<TechEra> era(UUID civ);

    EraDefinition.EraProgress eraProgress(UUID civ);

    int knowledge(UUID civ, Identifier tech);

    int cost(UUID civ, Identifier tech);

    void addKnowledge(UUID civ, Identifier tech, KnowledgeSourceType type, int amount, Entity actor);

    void fireTrigger(ServerPlayer player, Identifier trigger, Map<String, String> context);

    void addResearchPoints(UUID civ, int rp, Identifier reason);

    void grant(UUID civ, Identifier tech, Identifier reason);

    boolean advanceEra(UUID civ, boolean ceremony);

    float diffusionPressure(UUID civ, Identifier tech);

    void registerCostFactor(CostFactorProvider provider);

    void invalidateCost(UUID civ, Identifier tech);
}
