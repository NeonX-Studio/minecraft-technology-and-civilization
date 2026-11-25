package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.SettlementRecord;
import java.util.Objects;

public final class SettlementFoundedEvent extends TcEvent {
    private final SettlementRecord settlement;

    public SettlementFoundedEvent(SettlementRecord settlement) {
        this.settlement = Objects.requireNonNull(settlement, "settlement");
    }

    public SettlementRecord settlement() {
        return settlement;
    }
}
