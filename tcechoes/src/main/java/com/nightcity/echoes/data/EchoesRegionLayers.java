package com.nightcity.echoes.data;

import com.mojang.serialization.Codec;
import com.nightcity.echoes.TEchoes;
import com.nightcity.echoes.api.HistoryCell;
import com.nightcity.tcivilization.api.LayerSync;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionLayer;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Registration holder for the tcechoes:history RegionLayer (18 §3.1, §11.3).
 */
public final class EchoesRegionLayers {
    public static final Identifier HISTORY_LAYER_ID = Identifier.fromNamespaceAndPath(TEchoes.MOD_ID, "history");

    public static final RegionLayer<HistoryCell> HISTORY_LAYER = new RegionLayer<>() {
        @Override
        public Identifier id() {
            return HISTORY_LAYER_ID;
        }

        @Override
        public HistoryCell createDefault(RegionCell cell) {
            return HistoryCell.empty();
        }

        @Override
        public Codec<HistoryCell> codec() {
            return HistoryCell.CODEC;
        }

        @Override
        public LayerSync sync() {
            return LayerSync.NEAR;
        }

        @Override
        public Optional<StreamCodec<RegistryFriendlyByteBuf, HistoryCell>> streamCodec() {
            return Optional.of(HistoryCell.STREAM_CODEC);
        }
    };

    public static final List<RegionLayer<?>> ALL = List.of(HISTORY_LAYER);

    private EchoesRegionLayers() {
    }
}
