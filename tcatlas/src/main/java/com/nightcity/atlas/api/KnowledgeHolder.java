package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Owner of cartographic knowledge: individual player memory or shared civilization archive (22 §0.1, §11.1).
 */
public record KnowledgeHolder(Kind kind, UUID id) {
    public static final UUID DEFAULT_PLAYER_CIV_ID = UUID.nameUUIDFromBytes("tcivilization:player_civ".getBytes(StandardCharsets.UTF_8));
    public static final KnowledgeHolder PLAYER_CIVILIZATION = new KnowledgeHolder(Kind.CIVILIZATION, DEFAULT_PLAYER_CIV_ID);

    public enum Kind {
        PLAYER,
        CIVILIZATION,
        AI_CIV;

        public static final Codec<Kind> CODEC = Codec.STRING.comapFlatMap(
                raw -> {
                    try {
                        return DataResult.success(Kind.valueOf(raw.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ex) {
                        return DataResult.error(() -> "Unknown KnowledgeHolder.Kind: " + raw);
                    }
                },
                Enum::name
        );
    }

    public static final Codec<KnowledgeHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.fieldOf("kind").forGetter(KnowledgeHolder::kind),
            UUIDUtil.CODEC.fieldOf("id").forGetter(KnowledgeHolder::id)
    ).apply(instance, KnowledgeHolder::new));

    public KnowledgeHolder {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
    }

    public static KnowledgeHolder player(UUID playerId) {
        return new KnowledgeHolder(Kind.PLAYER, playerId);
    }

    public static KnowledgeHolder civilization(UUID civId) {
        return new KnowledgeHolder(Kind.CIVILIZATION, civId);
    }

    public long seedSalt() {
        return id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17) ^ (kind.ordinal() * 0x9E3779B97F4A7C15L);
    }

    public String folderName() {
        return kind.name().toLowerCase(Locale.ROOT) + "-" + id;
    }
}
