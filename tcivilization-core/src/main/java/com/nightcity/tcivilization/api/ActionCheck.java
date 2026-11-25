package com.nightcity.tcivilization.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Server-authoritative permission and gate check outcome (03 §12.1, 31 §16.1, 33 §14.1).
 */
public sealed interface ActionCheck permits ActionCheck.Allow, ActionCheck.Deny {
    record Allow() implements ActionCheck {
    }

    record Deny(Identifier reason, Component detail, Optional<String> fixLink) implements ActionCheck {
        public Deny {
            Objects.requireNonNull(reason, "reason");
            detail = detail == null ? Component.translatable("gui." + reason.getNamespace() + ".reason." + reason.getPath()) : detail;
            fixLink = fixLink == null ? Optional.empty() : fixLink;
        }
    }
}
