package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;

public final class CivPanelOpenedEvent extends Event {
    private final Identifier tabId;

    public CivPanelOpenedEvent(Identifier tabId) {
        this.tabId = Objects.requireNonNull(tabId, "tabId");
    }

    public Identifier tabId() {
        return tabId;
    }
}
