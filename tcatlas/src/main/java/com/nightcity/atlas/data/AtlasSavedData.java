package com.nightcity.atlas.data;

import com.mojang.serialization.Codec;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.map.SurveyAtlasStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-level SavedData wrapper persisting Atlas survey knowledge, waypoints, memory marks,
 * and VO-UI-01 initial locator_bar rule write flag with formatVersion = 1 (22 §11, 30 VO-UI-01).
 */
public final class AtlasSavedData extends SavedData {
    public static final int FORMAT_VERSION = AtlasPersistence.FORMAT_VERSION;
    public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "atlas_world_state");

    public static final Codec<AtlasSavedData> CODEC = CompoundTag.CODEC.xmap(
            AtlasSavedData::fromCompoundTag,
            AtlasSavedData::toCompoundTag
    );

    public static final SavedDataType<AtlasSavedData> TYPE = new SavedDataType<>(
            DATA_ID,
            AtlasSavedData::new,
            CODEC
    );

    private boolean locatorBarRuleApplied;
    private final Set<UUID> trackedPlayers = new LinkedHashSet<>();

    public AtlasSavedData() {
    }

    public static AtlasSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isLocatorBarRuleApplied() {
        return locatorBarRuleApplied;
    }

    public void markLocatorBarRuleApplied() {
        if (!this.locatorBarRuleApplied) {
            this.locatorBarRuleApplied = true;
            setDirty();
        }
    }

    public void trackPlayer(UUID playerId) {
        if (trackedPlayers.add(playerId)) {
            setDirty();
        }
    }

    public static AtlasSavedData fromCompoundTag(CompoundTag root) {
        AtlasSavedData data = new AtlasSavedData();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(SurveyAtlasStore.OVERWORLD_ID);
        data.locatorBarRuleApplied = root.getBooleanOr("locatorBarRuleApplied", false);
        if (root.contains("waypointsState")) {
            AtlasPersistence.decodeWaypointsState(root.getCompoundOrEmpty("waypointsState"), TAtlas.waypoints(), wrapX);
        }
        if (root.contains("surveyState")) {
            CompoundTag surveyTag = root.getCompoundOrEmpty("surveyState");
            AtlasPersistence.decodeSurveyState(surveyTag, TAtlas.surveyStore(), wrapX);
            CompoundTag mem = surveyTag.getCompoundOrEmpty("playerMemories");
            for (String k : mem.keySet()) {
                try {
                    data.trackedPlayers.add(UUID.fromString(k));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return data;
    }

    public CompoundTag toCompoundTag() {
        CompoundTag root = new CompoundTag();
        root.putInt(AtlasPersistence.FORMAT_VERSION_KEY, FORMAT_VERSION);
        root.putBoolean("locatorBarRuleApplied", locatorBarRuleApplied);
        Set<UUID> players = new LinkedHashSet<>(trackedPlayers);
        players.addAll(TAtlas.waypoints().exportAllMemoryMarks().keySet());
        root.put("waypointsState", AtlasPersistence.encodeWaypointsState(TAtlas.waypoints()));
        root.put("surveyState", AtlasPersistence.encodeSurveyState(
                TAtlas.surveyStore(),
                new ArrayList<>(players),
                List.of(SurveyAtlasStore.OVERWORLD_ID)
        ));
        return root;
    }
}
