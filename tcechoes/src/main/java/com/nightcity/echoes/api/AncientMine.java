package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.api.RegionApi;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * Ancient mine or quarry exploiting a surface-bearing deposit near historic settlements (18 §5.3.5, §11.1).
 */
public record AncientMine(
        int id,
        String depositId,
        BlockPos entrance,
        float minedFraction,
        int fromYear,
        int toYear,
        float weathering
) {
    public static final Codec<AncientMine> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(AncientMine::id),
            Codec.STRING.fieldOf("deposit_id").forGetter(AncientMine::depositId),
            BlockPos.CODEC.fieldOf("entrance").forGetter(AncientMine::entrance),
            Codec.FLOAT.fieldOf("mined_fraction").forGetter(AncientMine::minedFraction),
            Codec.INT.fieldOf("from_year").forGetter(AncientMine::fromYear),
            Codec.INT.fieldOf("to_year").forGetter(AncientMine::toYear),
            Codec.FLOAT.optionalFieldOf("weathering", 0.5F).forGetter(AncientMine::weathering)
    ).apply(i, AncientMine::new));

    public AncientMine {
        Objects.requireNonNull(depositId, "depositId");
        Objects.requireNonNull(entrance, "entrance");
        minedFraction = Math.clamp(minedFraction, 0.0F, 0.6F);
        weathering = Math.clamp(weathering, 0.0F, 1.0F);
    }

    public long regionKey() {
        return RegionApi.regionKey(Math.floorDiv(entrance.getX(), 512), Math.floorDiv(entrance.getZ(), 512));
    }
}
