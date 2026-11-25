package com.nightcity.tcivilization.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Dispatches ceremony presentations to online or offline civilization members (03 §12.1).
 */
public final class Ceremonies {
    private static final Map<UUID, List<CeremonySpec>> QUEUED = new ConcurrentHashMap<>();

    private Ceremonies() {
    }

    public static void play(ServerPlayer player, CeremonySpec spec) {
        if (player != null && spec != null) {
            playForUuid(player.getUUID(), spec);
        }
    }

    public static synchronized void playForUuid(UUID playerId, CeremonySpec spec) {
        QUEUED.computeIfAbsent(playerId, k -> new ArrayList<>()).add(spec);
    }

    public static synchronized List<CeremonySpec> drain(UUID playerId) {
        List<CeremonySpec> list = QUEUED.remove(playerId);
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }
}
