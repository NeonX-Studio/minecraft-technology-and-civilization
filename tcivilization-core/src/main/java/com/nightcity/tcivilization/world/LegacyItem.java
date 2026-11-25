package com.nightcity.tcivilization.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.TCivilization;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Replacement item for removed item IDs retaining original ID and count in a DataComponent (03 §5.9, §7).
 */
public class LegacyItem extends Item {
    public record LegacyData(Identifier originalId, int originalCount) {
        public static final LegacyData DEFAULT = new LegacyData(
                Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "unknown_item"), 1);

        public static final Codec<LegacyData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("original_id").forGetter(LegacyData::originalId),
                Codec.INT.optionalFieldOf("original_count", 1).forGetter(LegacyData::originalCount)
        ).apply(i, LegacyData::new));

        public static final StreamCodec<ByteBuf, LegacyData> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, LegacyData::originalId,
                ByteBufCodecs.VAR_INT, LegacyData::originalCount,
                LegacyData::new
        );

        public LegacyData {
            Objects.requireNonNull(originalId, "originalId");
            originalCount = Math.max(1, originalCount);
        }
    }

    public LegacyItem(Properties properties) {
        super(properties);
    }
}
