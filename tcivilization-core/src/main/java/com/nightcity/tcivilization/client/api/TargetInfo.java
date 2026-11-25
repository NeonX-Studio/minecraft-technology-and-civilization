package com.nightcity.tcivilization.client.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * Crosshair target summary (title, sourceMod, <= 3 lines, optional progress [0, 1], 03 §5.10.7).
 */
public record TargetInfo(
        Component title,
        String sourceModName,
        List<Component> lines,
        Optional<Float> progress
) {
    public TargetInfo {
        Objects.requireNonNull(title, "title");
        sourceModName = sourceModName == null ? "Minecraft" : sourceModName;
        lines = lines == null ? List.of() : lines.subList(0, Math.min(3, lines.size()));
        progress = progress == null ? Optional.empty() : progress.map(p -> Math.clamp(p, 0.0f, 1.0f));
    }
}
