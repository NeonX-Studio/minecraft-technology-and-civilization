package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Immutable civilization snapshot/record with PLAYER_CIV_ID constant (03 §5.1.1, §11).
 */
public record Civilization(
        UUID id,
        CivKind kind,
        String name,
        BannerPatternLayers banner,
        int eraIndex,
        long foundedTick,
        Map<UUID, CivMember> members,
        List<UUID> settlements,
        CivStats stats,
        Set<Identifier> flags
) implements VersionedShared {
    public static final UUID PLAYER_CIV_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String DEFAULT_NAME = "Nameless Tribe";

    public static final Codec<Civilization> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Civilization::id),
            CivKind.CODEC.fieldOf("kind").forGetter(Civilization::kind),
            Codec.string(1, 32).fieldOf("name").forGetter(Civilization::name),
            BannerPatternLayers.CODEC.optionalFieldOf("banner", BannerPatternLayers.EMPTY).forGetter(Civilization::banner),
            Codec.intRange(-1, 10).optionalFieldOf("era", -1).forGetter(Civilization::eraIndex),
            Codec.LONG.fieldOf("founded").forGetter(Civilization::foundedTick),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, CivMember.CODEC).optionalFieldOf("members", Map.of()).forGetter(Civilization::members),
            UUIDUtil.CODEC.listOf().optionalFieldOf("settlements", List.of()).forGetter(Civilization::settlements),
            CivStats.CODEC.optionalFieldOf("stats", new CivStats()).forGetter(Civilization::stats),
            Identifier.CODEC.listOf().xmap(LinkedHashSet::new, List::copyOf)
                    .optionalFieldOf("flags", new LinkedHashSet<>())
                    .forGetter(c -> new LinkedHashSet<>(c.flags))
    ).apply(i, Civilization::new));

    public Civilization {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        if (name == null || name.isBlank()) {
            name = DEFAULT_NAME;
        } else if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        banner = banner == null ? BannerPatternLayers.EMPTY : banner;
        if (eraIndex < -1 || eraIndex > 10) {
            throw new IllegalArgumentException("eraIndex out of [-1, 10]: " + eraIndex);
        }
        members = members == null ? Map.of() : Collections.unmodifiableCopy(members);
        settlements = settlements == null ? List.of() : List.copyOf(settlements);
        stats = stats == null ? new CivStats() : stats;
        flags = flags == null ? Set.of() : java.util.Collections.unmodifiableSet(new LinkedHashSet<>(flags));
    }

    public static Civilization defaultPlayerCiv(long foundedTick) {
        return new Civilization(
                PLAYER_CIV_ID,
                CivKind.PLAYER,
                DEFAULT_NAME,
                BannerPatternLayers.EMPTY,
                -1,
                foundedTick,
                Map.of(),
                List.of(),
                new CivStats(),
                Set.of()
        );
    }

    @Override
    public long version() {
        return stats.version();
    }

    private static final class Collections {
        private static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> src) {
            return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(src));
        }
    }
}
