package com.nightcity.tcivilization.person;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.api.PersonComponentType;
import com.nightcity.tcivilization.api.SimTier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * 24-slot daily schedule stub (1,000 ticks per slot; slot 0 = 06:00 morning, 03 §5.7.5).
 */
public record PersonSchedule(List<Identifier> slots, Optional<Identifier> overrideActivity, long overrideUntilTick) {
    public static final int SLOTS_PER_DAY = 24;
    public static final long TICKS_PER_SLOT = 1_000L;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "schedule");
    public static final Identifier SLEEP = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "sleep");
    public static final Identifier WORK = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "work");
    public static final Identifier MEAL = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "meal");
    public static final Identifier LEISURE = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "leisure");
    public static final Identifier SHELTER = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "shelter");
    public static final Identifier FOLLOW = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "follow");
    public static final Identifier WAIT = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "wait");

    public static final Codec<PersonSchedule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.listOf().fieldOf("slots").forGetter(PersonSchedule::slots),
            Identifier.CODEC.optionalFieldOf("override").forGetter(PersonSchedule::overrideActivity),
            Codec.LONG.optionalFieldOf("override_until", -1L).forGetter(PersonSchedule::overrideUntilTick)
    ).apply(i, PersonSchedule::new));

    public static final PersonComponentType<PersonSchedule> TYPE =
            PersonComponentType.simple(ID, CODEC, EnumSet.of(SimTier.L0, SimTier.L1));

    public PersonSchedule {
        List<Identifier> normalized = new ArrayList<>(SLOTS_PER_DAY);
        List<Identifier> defaults = defaultSlots();
        for (int idx = 0; idx < SLOTS_PER_DAY; idx++) {
            normalized.add(slots != null && idx < slots.size() && slots.get(idx) != null ? slots.get(idx) : defaults.get(idx));
        }
        slots = List.copyOf(normalized);
        overrideActivity = overrideActivity == null ? Optional.empty() : overrideActivity;
    }

    public static PersonSchedule standard() {
        return new PersonSchedule(defaultSlots(), Optional.empty(), -1L);
    }

    public static int slotAtDayTime(long dayTime) {
        long tickOfDay = Math.floorMod(dayTime, 24_000L);
        return (int) (tickOfDay / TICKS_PER_SLOT);
    }

    public Identifier activityAt(long dayTime) {
        if (overrideActivity.isPresent() && (overrideUntilTick < 0L || dayTime < overrideUntilTick)) {
            return overrideActivity.get();
        }
        return slots.get(slotAtDayTime(dayTime));
    }

    public PersonSchedule withOverride(Identifier activity, long untilTick) {
        return new PersonSchedule(slots, Optional.of(activity), untilTick);
    }

    public PersonSchedule clearOverride() {
        return new PersonSchedule(slots, Optional.empty(), -1L);
    }

    private static List<Identifier> defaultSlots() {
        List<Identifier> result = new ArrayList<>(SLOTS_PER_DAY);
        for (int s = 0; s < SLOTS_PER_DAY; s++) {
            if (s == 0 || s == 6 || s == 12) {
                result.add(MEAL);
            } else if (s >= 1 && s <= 9) {
                result.add(WORK);
            } else if (s >= 10 && s <= 11) {
                result.add(LEISURE);
            } else {
                result.add(SLEEP);
            }
        }
        return result;
    }
}
