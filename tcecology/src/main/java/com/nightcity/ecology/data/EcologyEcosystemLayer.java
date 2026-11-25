package com.nightcity.ecology.data;

import com.mojang.serialization.Codec;
import com.nightcity.tcivilization.api.LayerSync;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionLayer;
import net.minecraft.resources.Identifier;

public final class EcologyEcosystemLayer implements RegionLayer<EcologyRegionLayers.EmptyLayerData> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("tcecology", "ecosystem");
    public static final EcologyEcosystemLayer INSTANCE = new EcologyEcosystemLayer();

    private EcologyEcosystemLayer() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public EcologyRegionLayers.EmptyLayerData createDefault(RegionCell cell) {
        return new EcologyRegionLayers.EmptyLayerData(1);
    }

    @Override
    public Codec<EcologyRegionLayers.EmptyLayerData> codec() {
        return EcologyRegionLayers.EmptyLayerData.CODEC;
    }

    @Override
    public LayerSync sync() {
        return LayerSync.NEAR;
    }
}
