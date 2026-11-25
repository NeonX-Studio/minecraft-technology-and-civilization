package com.nightcity.technology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.technology.api.TechApi;
import com.nightcity.technology.data.CivTechState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TechnologySavedData extends SavedData {
    public static final int FORMAT_VERSION = 1;

    public static final Codec<TechnologySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("formatVersion", FORMAT_VERSION).forGetter(TechnologySavedData::formatVersion),
            CivTechState.CODEC.listOf().optionalFieldOf("civs", List.of()).forGetter(TechnologySavedData::civList),
            Codec.INT.optionalFieldOf("research_points").forGetter(d -> Optional.empty())
    ).apply(instance, TechnologySavedData::fromDecoded));

    public static final SavedDataType<TechnologySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, "technology_state"),
            TechnologySavedData::new,
            CODEC
    );

    private final int formatVersion;
    private final Map<UUID, CivTechState> civs;

    public TechnologySavedData() {
        this(FORMAT_VERSION, new LinkedHashMap<>());
        civ(TechApi.PLAYER_CIV_ID);
    }

    private TechnologySavedData(int formatVersion, Map<UUID, CivTechState> civs) {
        this.formatVersion = formatVersion;
        this.civs = civs;
    }

    private static TechnologySavedData fromDecoded(
            int version,
            List<CivTechState> storedCivs,
            Optional<Integer> legacyResearchPoints
    ) {
        Map<UUID, CivTechState> map = new LinkedHashMap<>();
        for (CivTechState state : storedCivs) {
            map.put(state.civId(), state);
        }
        CivTechState playerCiv = map.computeIfAbsent(TechApi.PLAYER_CIV_ID, CivTechState::new);
        if (legacyResearchPoints.isPresent() && legacyResearchPoints.get() > 0) {
            playerCiv.addPoolRp(legacyResearchPoints.get(), 100.0);
        }
        return new TechnologySavedData(FORMAT_VERSION, map);
    }

    public static TechnologySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int formatVersion() {
        return formatVersion;
    }

    public CivTechState civ(UUID civId) {
        return civs.computeIfAbsent(civId, id -> {
            setDirty();
            return new CivTechState(id);
        });
    }

    public Map<UUID, CivTechState> allCivs() {
        return Collections.unmodifiableMap(civs);
    }

    private List<CivTechState> civList() {
        return List.copyOf(civs.values());
    }

    public void addResearch(int amount) {
        CivTechState state = civ(TechApi.PLAYER_CIV_ID);
        double mult = TechConfig.SERVER_SPEC.isLoaded() ? TechConfig.POOL_CAP_MULTIPLIER.get() : 3.0;
        state.addPoolRp(amount, mult);
        setDirty();
    }

    public int researchPoints() {
        return civ(TechApi.PLAYER_CIV_ID).pool();
    }

    public void markModified() {
        setDirty();
    }
}
