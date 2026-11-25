package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record CivTabContext(
        String civName,
        int eraIndex,
        long dayTime,
        int memberCount,
        int population,
        long regionsVisited,
        Set<Identifier> unlockedTechs,
        TcSkin skin,
        boolean readOnly
) {
    public CivTabContext {
        civName = civName == null ? "Nameless Tribe" : civName;
        unlockedTechs = unlockedTechs == null ? Set.of() : Set.copyOf(unlockedTechs);
        Objects.requireNonNull(skin, "skin");
    }
}
