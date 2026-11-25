package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.nightcity.tcivilization.person.PersonEntity;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Component descriptor attachable to a PersonRecord (03 §5.7.3).
 */
public record PersonComponentType<T>(
        Identifier id,
        Codec<T> codec,
        Optional<StreamCodec<RegistryFriendlyByteBuf, T>> syncCodec,
        EnumSet<SimTier> tickTiers,
        BiConsumer<PersonRecord, PersonEntity> onEmbody,
        BiConsumer<PersonEntity, PersonRecord> onAbstract,
        Optional<Identifier> inspectSection
) {
    public PersonComponentType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");
        syncCodec = syncCodec == null ? Optional.empty() : syncCodec;
        tickTiers = tickTiers == null ? EnumSet.noneOf(SimTier.class) : EnumSet.copyOf(tickTiers);
        onEmbody = onEmbody == null ? (rec, ent) -> {} : onEmbody;
        onAbstract = onAbstract == null ? (ent, rec) -> {} : onAbstract;
        inspectSection = inspectSection == null ? Optional.empty() : inspectSection;
    }

    public static <T> PersonComponentType<T> simple(Identifier id, Codec<T> codec, EnumSet<SimTier> tickTiers) {
        return new PersonComponentType<>(id, codec, Optional.empty(), tickTiers, null, null, Optional.empty());
    }
}
