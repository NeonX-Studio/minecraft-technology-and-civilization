package com.nightcity.tcivilization.client.api;

import java.util.Objects;

public record ClientWheelContext(WheelTarget target, boolean hasPersonalTabs) {
    public ClientWheelContext {
        Objects.requireNonNull(target, "target");
    }
}
