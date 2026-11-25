package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * Client-side immutable HUD frame context (31 §3.2).
 */
public record HudContext(
        int eraIndex,
        Set<Identifier> unlockedTechs,
        HudLevel level,
        float scale,
        TcSkin skin,
        boolean highContrast,
        boolean reducedMotion,
        boolean inCombat,
        boolean inVehicle,
        String dimensionId,
        long dayTime
) {
    public HudContext {
        unlockedTechs = unlockedTechs == null ? Set.of() : Set.copyOf(unlockedTechs);
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(skin, "skin");
        dimensionId = dimensionId == null ? "minecraft:overworld" : dimensionId;
    }
}
