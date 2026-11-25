package com.nightcity.tcivilization.person;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.api.PersonComponentType;
import com.nightcity.tcivilization.api.SimTier;
import java.util.EnumSet;
import net.minecraft.resources.Identifier;

/**
 * Five-axis NPC needs component (03 §5.7.4). Applies exclusively to NPCs, never players (DR-19).
 */
public record PersonNeeds(float food, float rest, float social, float safety, float purpose) {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "needs");

    public static final Codec<PersonNeeds> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("food", 100.0f).forGetter(PersonNeeds::food),
            Codec.FLOAT.optionalFieldOf("rest", 100.0f).forGetter(PersonNeeds::rest),
            Codec.FLOAT.optionalFieldOf("social", 100.0f).forGetter(PersonNeeds::social),
            Codec.FLOAT.optionalFieldOf("safety", 100.0f).forGetter(PersonNeeds::safety),
            Codec.FLOAT.optionalFieldOf("purpose", 100.0f).forGetter(PersonNeeds::purpose)
    ).apply(i, PersonNeeds::new));

    public static final PersonComponentType<PersonNeeds> TYPE =
            PersonComponentType.simple(ID, CODEC, EnumSet.of(SimTier.L0, SimTier.L1, SimTier.L2));

    public PersonNeeds {
        food = Math.clamp(food, 0.0f, 100.0f);
        rest = Math.clamp(rest, 0.0f, 100.0f);
        social = Math.clamp(social, 0.0f, 100.0f);
        safety = Math.clamp(safety, 0.0f, 100.0f);
        purpose = Math.clamp(purpose, 0.0f, 100.0f);
    }

    public static PersonNeeds full() {
        return new PersonNeeds(100.0f, 100.0f, 100.0f, 100.0f, 100.0f);
    }

    public float mood() {
        float raw = 0.30f * food + 0.25f * rest + 0.15f * social + 0.20f * safety + 0.10f * purpose;
        return Math.clamp(raw, 0.0f, 100.0f);
    }

    public float workEfficiency() {
        float eff = 1.0f;
        if (food < 30.0f) {
            eff *= 0.7f;
        }
        if (rest < 30.0f) {
            eff *= 0.8f;
        }
        return Math.max(0.4f, eff);
    }

    public PersonNeeds decayFractionOfDay(float dayFraction, boolean sleeping, boolean hasJob) {
        float nextFood = food - 40.0f * dayFraction;
        float nextRest = rest + (sleeping ? 80.0f : -50.0f) * dayFraction;
        float nextSocial = social - 20.0f * dayFraction;
        float nextPurpose = purpose - (hasJob ? 10.0f : 30.0f) * dayFraction;
        return new PersonNeeds(nextFood, nextRest, nextSocial, safety, nextPurpose);
    }
}
