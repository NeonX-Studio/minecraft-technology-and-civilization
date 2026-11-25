package com.nightcity.technology.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;

public interface MilestoneChecker {
    record MilestoneStatus(boolean completed, float progress, String detail) {
        public static final MilestoneStatus INCOMPLETE = new MilestoneStatus(false, 0.0f, "");
        public static final MilestoneStatus COMPLETE = new MilestoneStatus(true, 1.0f, "completed");
    }

    interface ContactProvider {
        Set<UUID> contacts(UUID civ);
    }

    Identifier milestone();

    MilestoneStatus check(UUID civ, ServerLevel level);
}
