package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ancient overland road or sea trade route polyline across region keys (18 §5.3.4, §11.1).
 * Each point is a packed 512-block regionKey (rx << 32 | rz), capped at 64 waypoints.
 */
public record Route(
        int id,
        RouteKind kind,
        int polityA,
        int polityB,
        int fromYear,
        int toYear,
        List<Long> points,
        float weathering
) {
    public static final int MAX_POINTS = 64;

    public static final Codec<Route> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Route::id),
            RouteKind.CODEC.fieldOf("kind").forGetter(Route::kind),
            Codec.INT.fieldOf("polity_a").forGetter(Route::polityA),
            Codec.INT.fieldOf("polity_b").forGetter(Route::polityB),
            Codec.INT.fieldOf("from_year").forGetter(Route::fromYear),
            Codec.INT.fieldOf("to_year").forGetter(Route::toYear),
            Codec.LONG.listOf().fieldOf("points").forGetter(Route::points),
            Codec.FLOAT.optionalFieldOf("weathering", 0.5F).forGetter(Route::weathering)
    ).apply(i, Route::new));

    public Route {
        Objects.requireNonNull(kind, "kind");
        if (points == null || points.isEmpty()) {
            points = List.of();
        } else if (points.size() > MAX_POINTS) {
            points = List.copyOf(points.subList(0, MAX_POINTS));
        } else {
            points = List.copyOf(points);
        }
        weathering = Math.clamp(weathering, 0.0F, 1.0F);
    }

    public Route(int id, RouteKind kind, int polityA, int polityB, int fromYear, int toYear, long[] rawPoints, float weathering) {
        this(id, kind, polityA, polityB, fromYear, toYear, toLongList(rawPoints), weathering);
    }

    public long[] pointsArray() {
        long[] out = new long[points.size()];
        for (int i = 0; i < points.size(); i++) {
            out[i] = points.get(i);
        }
        return out;
    }

    private static List<Long> toLongList(long[] arr) {
        if (arr == null || arr.length == 0) {
            return List.of();
        }
        int len = Math.min(arr.length, MAX_POINTS);
        List<Long> list = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            list.add(arr[i]);
        }
        return list;
    }
}
