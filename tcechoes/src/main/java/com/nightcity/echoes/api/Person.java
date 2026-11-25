package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;

/**
 * Named historical figure generated during pre-simulation (18 §5.1.7, §11.1).
 */
public record Person(
        int id,
        int polityId,
        String name,
        String epithet,
        int born,
        int died,
        PersonRole role,
        List<Integer> deeds
) {
    public static final Codec<Person> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Person::id),
            Codec.INT.fieldOf("polity_id").forGetter(Person::polityId),
            Codec.STRING.fieldOf("name").forGetter(Person::name),
            Codec.STRING.optionalFieldOf("epithet", "").forGetter(Person::epithet),
            Codec.INT.fieldOf("born").forGetter(Person::born),
            Codec.INT.fieldOf("died").forGetter(Person::died),
            PersonRole.CODEC.fieldOf("role").forGetter(Person::role),
            Codec.INT.listOf().optionalFieldOf("deeds", List.of()).forGetter(Person::deeds)
    ).apply(i, Person::new));

    public Person {
        Objects.requireNonNull(name, "name");
        epithet = epithet == null ? "" : epithet;
        Objects.requireNonNull(role, "role");
        deeds = deeds == null ? List.of() : List.copyOf(deeds);
    }
}
