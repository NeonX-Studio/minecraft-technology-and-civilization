package com.nightcity.tcivilization.api.world;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Core-hosted geological prospecting contract (10 §9, §12.1).
 */
public interface ProspectingApi {
    ProspectReading query(ServerPlayer player, ProspectMethod method, BlockPos pos, int radius);

    ProspectReading queryAbstract(UUID civ, ProspectMethod method, BlockPos pos, int radius);
}
