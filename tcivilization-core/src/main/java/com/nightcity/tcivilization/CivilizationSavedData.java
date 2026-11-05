package com.nightcity.tcivilization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class CivilizationSavedData extends SavedData {
    public static final SavedDataType<CivilizationSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "civilization_state"),
            CivilizationSavedData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("world_ticks").orElse(0L).forGetter(data -> data.worldTicks),
                    Codec.INT.fieldOf("login_events").orElse(0).forGetter(data -> data.loginEvents),
                    Codec.INT.fieldOf("peak_players").orElse(0).forGetter(data -> data.peakPlayers)
            ).apply(instance, CivilizationSavedData::new))
    );

    private long worldTicks;
    private int loginEvents;
    private int peakPlayers;

    public CivilizationSavedData() {
        this(0L, 0, 0);
    }

    private CivilizationSavedData(long worldTicks, int loginEvents, int peakPlayers) {
        this.worldTicks = worldTicks;
        this.loginEvents = loginEvents;
        this.peakPlayers = peakPlayers;
    }

    public static CivilizationSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void recordLogin() {
        loginEvents++;
        setDirty();
    }

    public void recordSecond(MinecraftServer server) {
        worldTicks += 20L;
        peakPlayers = Math.max(peakPlayers, server.getPlayerList().getPlayerCount());
        setDirty();
    }

    public long worldTicks() {
        return worldTicks;
    }

    public int loginEvents() {
        return loginEvents;
    }

    public int peakPlayers() {
        return peakPlayers;
    }
}
