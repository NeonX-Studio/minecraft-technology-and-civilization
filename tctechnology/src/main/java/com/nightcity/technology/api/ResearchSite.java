package com.nightcity.technology.api;

import net.minecraft.resources.Identifier;

import java.util.UUID;

public interface ResearchSite {
    record ResearchSiteContext(TechEra currentEra, int siteOrdinal, int booksCount, boolean presided, double skillMultiplier) {
    }

    Identifier siteType();

    UUID civ();

    double rpPerDay(ResearchSiteContext ctx);

    default int hearthSlotCost() {
        return 0;
    }
}
