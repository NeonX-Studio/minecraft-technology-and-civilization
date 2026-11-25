package com.nightcity.tcivilization.api.world;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Region-level deposit and remaining tonnage summary (10 §12.1).
 */
public record DepositSummary(
        long regionKey,
        List<Deposit> deposits,
        Map<Identifier, Long> remainingTonnageByMineral
) {
    public DepositSummary {
        deposits = deposits == null ? List.of() : List.copyOf(deposits);
        remainingTonnageByMineral = remainingTonnageByMineral == null ? Map.of() : Map.copyOf(remainingTonnageByMineral);
    }

    public static DepositSummary empty(long regionKey) {
        return new DepositSummary(regionKey, List.of(), Map.of());
    }
}
