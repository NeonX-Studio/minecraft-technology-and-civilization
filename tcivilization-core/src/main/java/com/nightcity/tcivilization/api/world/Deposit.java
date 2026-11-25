package com.nightcity.tcivilization.api.world;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Global mineral deposit descriptor (10 §5.10.4, §12.1).
 */
public record Deposit(
        long id,
        DepositType type,
        Identifier mineral,
        List<Identifier> byproducts,
        BlockPos center,
        int radiusXZ,
        int radiusY,
        float strikeDeg,
        float dipDeg,
        Identifier hostRock,
        float gradeMean,
        long tonnage,
        boolean discovered,
        EnumSet<Sign> surfaceSigns
) {
    public enum Sign {
        LOOSE_STONES,
        ORE_FLOAT,
        GOSSAN,
        MALACHITE_STAIN,
        ALTERATION,
        FUMAROLE,
        HOT_SPRING,
        PEGMATITE_OUTCROP,
        MARBLE_CONTACT,
        MAGNETIC_ANOMALY,
        IRON_RIDGE,
        BOG_STAIN,
        ULTRAMAFIC_OUTCROP,
        LATERITE_MESA,
        PLACER_HEAVY,
        SALT_CRUST,
        FOSSIL_BED,
        QUARTZ_VEIN,
        BLUE_GROUND,
        GEODE,
        CHALK_CLIFF,
        COAL_SEAM,
        KAOLIN_CLAY,
        GRAPHITE_OUTCROP,
        BITUMEN_SEEP,
        GAS_SEEP
    }

    public Deposit {
        byproducts = byproducts == null ? List.of() : List.copyOf(byproducts);
        surfaceSigns = surfaceSigns == null ? EnumSet.noneOf(Sign.class) : EnumSet.copyOf(surfaceSigns);
    }
}
