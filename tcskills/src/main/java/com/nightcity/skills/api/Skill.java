package com.nightcity.skills.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum Skill {
    FORAGING("foraging", Group.GATHERING, "yield/forage", "speed/forage"),
    HUNTING("hunting", Group.GATHERING, "yield/animal_drop", "precision/tracking"),
    WOODCUTTING("woodcutting", Group.GATHERING, "speed/axe", "yield/log"),
    MINING("mining", Group.GATHERING, "speed/pickaxe", "wear/pickaxe"),
    FARMING("farming", Group.PROVISION, "yield/crop", "speed/crop"),
    HUSBANDRY("husbandry", Group.PROVISION, "yield/animal_product", "speed/breeding"),
    COOKING("cooking", Group.PROVISION, "quality/food", "cost/fuel"),
    MEDICINE("medicine", Group.PROVISION, "precision/treatment", "speed/treatment"),
    CRAFTING("crafting", Group.MAKING, "speed/handcraft", "cost/handcraft"),
    SMITHING("smithing", Group.MAKING, "quality/metal", "cost/fuel"),
    ENGINEERING("engineering", Group.MAKING, "wear/machine", "speed/assembly"),
    CONSTRUCTION("construction", Group.MAKING, "speed/build", "cost/build"),
    LOGISTICS("logistics", Group.SOCIETY, "capacity/cargo", "speed/vehicle"),
    TRADE("trade", Group.SOCIETY, "price/trade", "capacity/caravan"),
    DIPLOMACY("diplomacy", Group.SOCIETY, "influence/relation", "speed/negotiation"),
    LEADERSHIP("leadership", Group.SOCIETY, "capacity/command", "influence/morale"),
    COMBAT("combat", Group.SOCIETY, "wear/weapon", "precision/aim"),
    SCHOLARSHIP("scholarship", Group.INQUIRY, "speed/research", "precision/experiment"),
    SURVEYING("surveying", Group.INQUIRY, "precision/survey", "range/survey"),
    EXPLORATION("exploration", Group.INQUIRY, "range/reveal", "yield/loot"),
    AERONAUTICS("aeronautics", Group.INQUIRY, "cost/propellant", "precision/flight"),
    ARCANA("arcana", Group.INQUIRY, "precision/ritual", "cost/anomaly");

    public enum Group {
        GATHERING, PROVISION, MAKING, SOCIETY, INQUIRY
    }

    private static final Map<String, Skill> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Skill::shortId, Function.identity()));

    public static final Codec<Skill> CODEC = Codec.STRING.comapFlatMap(
            raw -> resolve(raw).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown skill: " + raw)),
            Skill::shortId
    );

    private final String shortId;
    private final Identifier id;
    private final Group group;
    private final String primaryEffect;
    private final String secondaryEffect;

    Skill(String shortId, Group group, String primaryEffect, String secondaryEffect) {
        this.shortId = shortId;
        this.id = Identifier.fromNamespaceAndPath("tcskills", shortId);
        this.group = group;
        this.primaryEffect = primaryEffect;
        this.secondaryEffect = secondaryEffect;
    }

    public String shortId() {
        return shortId;
    }

    public Identifier id() {
        return id;
    }

    public Group group() {
        return group;
    }

    public String primaryEffect() {
        return primaryEffect;
    }

    public String secondaryEffect() {
        return secondaryEffect;
    }

    public String translationKey() {
        return "skill.tcskills." + shortId;
    }

    public boolean isActive(Predicate<String> isModLoaded) {
        return this != ARCANA || isModLoaded.test("tcbeyond");
    }

    public static Optional<Skill> resolve(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        String normalized = raw.startsWith("tcskills:") ? raw.substring("tcskills:".length()) : raw;
        if (normalized.contains(":")) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(normalized));
    }
}
