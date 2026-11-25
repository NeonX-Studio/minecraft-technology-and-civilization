package com.nightcity.skills.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.skills.api.Skill;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

public record TalentDef(
        Identifier id,
        Skill skill,
        Slot slot,
        int cost,
        int level,
        Optional<Identifier> eraGate,
        Optional<Identifier> exclusiveWith,
        List<Modifier> modifiers,
        List<OpGrant> ops,
        Optional<String> requiresMod,
        Optional<Fallback> fallback,
        Identifier icon
) {
    public enum Slot {
        T1("t1", 1, 5),
        T2("t2", 1, 12),
        T3A("t3a", 2, 20),
        T3B("t3b", 2, 20),
        T4("t4", 2, 28),
        T5("t5", 3, 36),
        C("c", 4, 45);

        public static final Codec<Slot> CODEC = Codec.STRING.comapFlatMap(
                raw -> {
                    for (Slot s : values()) {
                        if (s.jsonKey.equalsIgnoreCase(raw)) {
                            return DataResult.success(s);
                        }
                    }
                    return DataResult.error(() -> "Unknown talent slot: " + raw);
                },
                Slot::jsonKey
        );

        private final String jsonKey;
        private final int expectedCost;
        private final int expectedLevel;

        Slot(String jsonKey, int expectedCost, int expectedLevel) {
            this.jsonKey = jsonKey;
            this.expectedCost = expectedCost;
            this.expectedLevel = expectedLevel;
        }

        public String jsonKey() {
            return jsonKey;
        }

        public int expectedCost() {
            return expectedCost;
        }

        public int expectedLevel() {
            return expectedLevel;
        }
    }

    public record Modifier(Identifier effect, float amount) {
        public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("effect").forGetter(Modifier::effect),
                Codec.FLOAT.fieldOf("amount").forGetter(Modifier::amount)
        ).apply(i, Modifier::new));
    }

    public record OpGrant(Identifier op, float value) {
        public static final Codec<OpGrant> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("op").forGetter(OpGrant::op),
                Codec.FLOAT.fieldOf("value").forGetter(OpGrant::value)
        ).apply(i, OpGrant::new));
    }

    public record Fallback(List<Modifier> modifiers, List<OpGrant> ops) {
        public static final Codec<Fallback> CODEC = RecordCodecBuilder.create(i -> i.group(
                Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(Fallback::modifiers),
                OpGrant.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(Fallback::ops)
        ).apply(i, Fallback::new));

        public boolean isEmpty() {
            return modifiers.isEmpty() && ops.isEmpty();
        }
    }

    public record RawTalentJson(
            Skill skill,
            Slot slot,
            int cost,
            int level,
            Optional<Identifier> eraGate,
            Optional<Identifier> exclusiveWith,
            List<Modifier> modifiers,
            List<OpGrant> ops,
            Optional<String> requiresMod,
            Optional<Fallback> fallback,
            Identifier icon
    ) {
        public static final Codec<RawTalentJson> CODEC = RecordCodecBuilder.create(i -> i.group(
                Skill.CODEC.fieldOf("skill").forGetter(RawTalentJson::skill),
                Slot.CODEC.fieldOf("slot").forGetter(RawTalentJson::slot),
                Codec.INT.fieldOf("cost").forGetter(RawTalentJson::cost),
                Codec.INT.fieldOf("level").forGetter(RawTalentJson::level),
                Identifier.CODEC.optionalFieldOf("era_gate").forGetter(RawTalentJson::eraGate),
                Identifier.CODEC.optionalFieldOf("exclusive_with").forGetter(RawTalentJson::exclusiveWith),
                Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(RawTalentJson::modifiers),
                OpGrant.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(RawTalentJson::ops),
                Codec.STRING.optionalFieldOf("requires_mod").forGetter(RawTalentJson::requiresMod),
                Fallback.CODEC.optionalFieldOf("fallback").forGetter(RawTalentJson::fallback),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.withDefaultNamespace("book")).forGetter(RawTalentJson::icon)
        ).apply(i, RawTalentJson::new));
    }

    public List<Modifier> effectiveModifiers(Predicate<String> isModLoaded) {
        if (requiresMod.isPresent() && !isModLoaded.test(requiresMod.get())) {
            return fallback.map(Fallback::modifiers).orElse(List.of());
        }
        return modifiers;
    }

    public List<OpGrant> effectiveOps(Predicate<String> isModLoaded) {
        if (requiresMod.isPresent() && !isModLoaded.test(requiresMod.get())) {
            return fallback.map(Fallback::ops).orElse(List.of());
        }
        return ops;
    }

    public int requiredEraIndex() {
        if (eraGate.isEmpty()) {
            return 0;
        }
        String p = eraGate.get().getPath().toLowerCase(Locale.ROOT);
        if (p.startsWith("e")) {
            try {
                return Integer.parseInt(p.substring(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
