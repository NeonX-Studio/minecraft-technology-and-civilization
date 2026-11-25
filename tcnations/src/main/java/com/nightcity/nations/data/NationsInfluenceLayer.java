package com.nightcity.nations.data;

import com.mojang.serialization.Codec;
import com.nightcity.tcivilization.api.LayerSync;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionLayer;
import net.minecraft.resources.Identifier;

public final class NationsInfluenceLayer implements RegionLayer<InfluenceLayerData> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("tcnations", "influence");
    public static final NationsInfluenceLayer INSTANCE = new NationsInfluenceLayer();

    private NationsInfluenceLayer() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public InfluenceLayerData createDefault(RegionCell cell) {
        return InfluenceLayerData.empty();
    }

    @Override
    public Codec<InfluenceLayerData> codec() {
        return InfluenceLayerData.CODEC;
    }

    @Override
    public LayerSync sync() {
        return LayerSync.ON_DEMAND;
    }
}
