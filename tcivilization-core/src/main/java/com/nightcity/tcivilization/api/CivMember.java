package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.TCivilization;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Player membership record in PLAYER_CIV_ID (03 §5.1.2, §11).
 */
public record CivMember(
        UUID playerId,
        Identifier role,
        long joinedTick,
        List<TrackerPin> pins,
        long chronicleCursor,
        Map<Identifier, Long> contribution
) {
    public static final Identifier ROLE_FOUNDER = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "founder");
    public static final Identifier ROLE_MEMBER = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "member");
    public static final Identifier ROLE_GUEST = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "guest");

    public static final Codec<CivMember> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(CivMember::playerId),
            Identifier.CODEC.fieldOf("role").forGetter(CivMember::role),
            Codec.LONG.fieldOf("joined_tick").forGetter(CivMember::joinedTick),
            TrackerPin.CODEC.listOf().optionalFieldOf("pins", List.of()).forGetter(CivMember::pins),
            Codec.LONG.optionalFieldOf("chronicle_cursor", 0L).forGetter(CivMember::chronicleCursor),
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("contribution", Map.of()).forGetter(CivMember::contribution)
    ).apply(instance, CivMember::new));

    public CivMember {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(role, "role");
        pins = pins == null ? List.of() : List.copyOf(pins);
        contribution = contribution == null ? Map.of() : Map.copyOf(contribution);
    }

    public CivMember withRole(Identifier nextRole) {
        return new CivMember(playerId, nextRole, joinedTick, pins, chronicleCursor, contribution);
    }

    public CivMember withPins(List<TrackerPin> nextPins) {
        return new CivMember(playerId, role, joinedTick, nextPins, chronicleCursor, contribution);
    }
}
