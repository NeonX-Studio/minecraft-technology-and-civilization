package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.SettlementRecord;
import java.util.Objects;

public final class SettlementTierChangedEvent extends TcEvent {
    private final SettlementRecord settlement;
    private final int oldTier;
    private final int newTier;

    public SettlementTierChangedEvent(SettlementRecord settlement, int oldTier, int newTier) {
        this.settlement = Objects.requireNonNull(settlement, "settlement");
        this.oldTier = oldTier;
        this.newTier = newTier;
    }

    public SettlementRecord settlement() {
        return settlement;
    }

    public int oldTier() {
        return oldTier;
    }

    public int newTier() {
        return newTier;
    }
}
