package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public record SectionSpec(
        Identifier id,
        int order,
        Optional<Identifier> requiredComponent,
        Supplier<PersonSection> factory
) {
    public interface PersonSection {
        void render(GuiGraphicsExtractor graphics, HudRect area, float partialTick);
    }

    public SectionSpec {
        Objects.requireNonNull(id, "id");
        requiredComponent = requiredComponent == null ? Optional.empty() : requiredComponent;
        Objects.requireNonNull(factory, "factory");
    }
}
