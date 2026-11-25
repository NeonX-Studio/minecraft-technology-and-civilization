package com.nightcity.tcivilization.api;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * One-shot notification payload dispatched via TcNotifications (03 §5.10.5, §11).
 */
public record Notification(
        Identifier category,
        Severity severity,
        Component title,
        @Nullable Component body,
        @Nullable Identifier icon,
        @Nullable String link,
        String mergeKey,
        int ttlTicks
) {
    public Notification {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(title, "title");
        mergeKey = mergeKey == null ? "" : mergeKey;
        ttlTicks = ttlTicks <= 0 ? severity.defaultTtlTicks() : ttlTicks;
    }
}
