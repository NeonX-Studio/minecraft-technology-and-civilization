package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

/**
 * Cultural name structure for a PersonRecord (03 §5.7.2).
 */
public record PersonName(String given, String family, Optional<String> epithet) {
    public static final Codec<PersonName> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("given").forGetter(PersonName::given),
            Codec.STRING.optionalFieldOf("family", "").forGetter(PersonName::family),
            Codec.STRING.optionalFieldOf("epithet").forGetter(PersonName::epithet)
    ).apply(i, PersonName::new));

    public PersonName {
        Objects.requireNonNull(given, "given");
        family = family == null ? "" : family;
        epithet = epithet == null ? Optional.empty() : epithet;
    }

    public String fullName() {
        String base = family.isEmpty() ? given : (family + " " + given);
        return epithet.map(ep -> base + " · 「" + ep + "」").orElse(base);
    }
}
