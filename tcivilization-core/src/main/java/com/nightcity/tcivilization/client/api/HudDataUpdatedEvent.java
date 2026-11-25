package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;

public final class HudDataUpdatedEvent extends Event {
    private final Identifier channel;

    public HudDataUpdatedEvent(Identifier channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    public Identifier channel() {
        return channel;
    }
}
