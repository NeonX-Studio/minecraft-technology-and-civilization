package com.nightcity.tcivilization.api;

import com.nightcity.tcivilization.TCivilization;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;

public record PersonSpec(
        UUID civ,
        Optional<UUID> settlement,
        PersonName name,
        int birthDay,
        Identifier culture,
        long appearance,
        GlobalPos initialPos,
        EnumSet<ImportanceFlag> importance
) {
    public static final Identifier DEFAULT_CULTURE = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "default");

    public PersonSpec {
        Objects.requireNonNull(civ, "civ");
        settlement = settlement == null ? Optional.empty() : settlement;
        Objects.requireNonNull(name, "name");
        culture = culture == null ? DEFAULT_CULTURE : culture;
        Objects.requireNonNull(initialPos, "initialPos");
        importance = importance == null ? EnumSet.noneOf(ImportanceFlag.class) : EnumSet.copyOf(importance);
    }
}
