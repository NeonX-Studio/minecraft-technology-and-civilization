package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.SettlementRecord;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class SettlementRemovedEvent extends TcEvent {
    private final SettlementRecord settlement;
    private final Identifier reason;

    public SettlementRemovedEvent(SettlementRecord settlement, Identifier reason) {
        this.settlement = Objects.requireNonNull(settlement, "settlement");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public SettlementRecord settlement() {
        return settlement;
    }

    public Identifier reason() {
        return reason;
    }
}
