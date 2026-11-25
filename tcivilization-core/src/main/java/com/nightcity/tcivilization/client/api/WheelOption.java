package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One command wheel sector option (03 §5.10.3, §12.2, 31 §5.3).
 */
public record WheelOption(
        Identifier id,
        Identifier icon,
        Component label,
        int priority,
        boolean enabled,
        Optional<Component> disabledReason,
        boolean needsConfirm,
        WheelSlotHint slotHint
) {
    public WheelOption {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(label, "label");
        disabledReason = disabledReason == null ? Optional.empty() : disabledReason;
        slotHint = slotHint == null ? WheelSlotHint.AUTO : slotHint;
    }
}
