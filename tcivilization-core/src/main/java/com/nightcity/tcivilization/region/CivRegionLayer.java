package com.nightcity.tcivilization.region;

import com.mojang.serialization.Codec;
import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.api.LayerSync;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionLayer;
import net.minecraft.resources.Identifier;

/**
 * Built-in tcivilization:civ region layer (03 §5.2.3).
 */
public final class CivRegionLayer implements RegionLayer<CivRegionData> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "civ");
    public static final CivRegionLayer INSTANCE = new CivRegionLayer();

    private CivRegionLayer() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public CivRegionData createDefault(RegionCell cell) {
        return CivRegionData.empty();
    }

    @Override
    public Codec<CivRegionData> codec() {
        return CivRegionData.CODEC;
    }

    @Override
    public LayerSync sync() {
        return LayerSync.NEAR;
    }
}
