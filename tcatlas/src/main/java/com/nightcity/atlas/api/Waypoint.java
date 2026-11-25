package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Waypoint record with Okabe-Ito color + distinct shape and split MapCodec serialization (22 §5.14, §11.1).
 */
public record Waypoint(
        UUID id,
        String name,
        OkabeItoPalette.WaypointCategory category,
        Identifier icon,
        int argb,
        OkabeItoPalette.WaypointShape shape,
        Share share,
        Identifier dimension,
        BlockPos pos,
        int epsilon,
        UUID creator,
        long createdDay,
        Optional<String> note,
        Optional<Integer> routeOrder
) {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_NOTE_LENGTH = 128;

    public enum Share {
        PRIVATE,
        GROUP,
        CIVILIZATION;

        public static final Codec<Share> CODEC = Codec.STRING.comapFlatMap(
                raw -> {
                    try {
                        return DataResult.success(Share.valueOf(raw.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ex) {
                        return DataResult.error(() -> "Unknown Share: " + raw);
                    }
                },
                Enum::name
        );
    }

    private record VisualPart(
            UUID id,
            String name,
            OkabeItoPalette.WaypointCategory category,
            Identifier icon,
            int argb,
            OkabeItoPalette.WaypointShape shape,
            Share share
    ) {
        static final MapCodec<VisualPart> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(VisualPart::id),
                Codec.STRING.fieldOf("name").forGetter(VisualPart::name),
                OkabeItoPalette.WaypointCategory.CODEC.fieldOf("category").forGetter(VisualPart::category),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.fromNamespaceAndPath("tcatlas", "marker")).forGetter(VisualPart::icon),
                Codec.INT.fieldOf("argb").forGetter(VisualPart::argb),
                OkabeItoPalette.WaypointShape.CODEC.fieldOf("shape").forGetter(VisualPart::shape),
                Share.CODEC.optionalFieldOf("share", Share.PRIVATE).forGetter(VisualPart::share)
        ).apply(i, VisualPart::new));
    }

    private record SpatialPart(
            Identifier dimension,
            BlockPos pos,
            int epsilon,
            UUID creator,
            long createdDay,
            Optional<String> note,
            Optional<Integer> routeOrder
    ) {
        static final MapCodec<SpatialPart> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.optionalFieldOf("dim", Identifier.withDefaultNamespace("overworld")).forGetter(SpatialPart::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(SpatialPart::pos),
                Codec.INT.optionalFieldOf("eps", 8).forGetter(SpatialPart::epsilon),
                UUIDUtil.CODEC.fieldOf("creator").forGetter(SpatialPart::creator),
                Codec.LONG.optionalFieldOf("created", 0L).forGetter(SpatialPart::createdDay),
                Codec.STRING.optionalFieldOf("note").forGetter(SpatialPart::note),
                Codec.INT.optionalFieldOf("route").forGetter(SpatialPart::routeOrder)
        ).apply(i, SpatialPart::new));
    }

    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VisualPart.MAP_CODEC.forGetter(w -> new VisualPart(w.id, w.name, w.category, w.icon, w.argb, w.shape, w.share)),
            SpatialPart.MAP_CODEC.forGetter(w -> new SpatialPart(w.dimension, w.pos, w.epsilon, w.creator, w.createdDay, w.note, w.routeOrder))
    ).apply(instance, (v, s) -> new Waypoint(
            v.id, v.name, v.category, v.icon, v.argb, v.shape, v.share,
            s.dimension, s.pos, s.epsilon, s.creator, s.createdDay, s.note, s.routeOrder
    )));

    public Waypoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(share, "share");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(creator, "creator");
        icon = icon == null ? category.identifier() : icon;
        String trimmed = name == null || name.isBlank() ? "Waypoint" : name.trim();
        name = trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
        if ((argb & 0xFF000000) == 0) {
            argb |= 0xFF000000;
        }
        epsilon = Math.max(0, epsilon);
        note = note == null ? Optional.empty() : note.map(n -> n.length() > MAX_NOTE_LENGTH ? n.substring(0, MAX_NOTE_LENGTH) : n);
        routeOrder = routeOrder == null ? Optional.empty() : routeOrder;
    }
}
