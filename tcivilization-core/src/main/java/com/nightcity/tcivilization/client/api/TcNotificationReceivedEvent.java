package com.nightcity.tcivilization.client.api;

import com.nightcity.tcivilization.api.Notification;
import java.util.Objects;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public final class TcNotificationReceivedEvent extends Event implements ICancellableEvent {
    private final Notification notification;

    public TcNotificationReceivedEvent(Notification notification) {
        this.notification = Objects.requireNonNull(notification, "notification");
    }

    public Notification notification() {
        return notification;
    }
}
