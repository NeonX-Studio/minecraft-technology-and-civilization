package com.nightcity.technology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class TechnologySavedData extends SavedData {
    public static final SavedDataType<TechnologySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, "technology_state"),
            TechnologySavedData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("research_points").orElse(0).forGetter(data -> data.researchPoints)
            ).apply(instance, TechnologySavedData::new))
    );

    private int researchPoints;

    public TechnologySavedData() {
        this(0);
    }

    private TechnologySavedData(int researchPoints) {
        this.researchPoints = researchPoints;
    }

    public static TechnologySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void addResearch(int amount) {
        researchPoints = Math.max(0, researchPoints + amount);
        setDirty();
    }

    public int researchPoints() {
        return researchPoints;
    }
}
