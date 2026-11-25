package com.nightcity.technology.api;

import net.minecraft.resources.Identifier;

import java.util.UUID;

public interface CostFactorProvider {
    Identifier id();

    double factor(UUID civ, Identifier tech);
}
