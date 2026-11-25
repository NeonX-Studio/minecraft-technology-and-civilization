package com.nightcity.tcivilization.api;

import java.util.UUID;
import net.minecraft.core.GlobalPos;
import org.jspecify.annotations.Nullable;

public record ActionScope(@Nullable UUID civ, @Nullable UUID object, @Nullable GlobalPos pos) {
}
