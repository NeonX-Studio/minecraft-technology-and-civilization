package com.nightcity.technology.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TechEra {
    STONE(0, "stone", 18, 20, 10, 30),
    NEOLITHIC(1, "neolithic", 26, 60, 30, 60),
    BRONZE(2, "bronze", 30, 150, 45, 110),
    IRON(3, "iron", 32, 320, 70, 180),
    MEDIEVAL(4, "medieval", 36, 650, 100, 300),
    RENAISSANCE(5, "renaissance", 36, 1200, 140, 480),
    INDUSTRIAL(6, "industrial", 40, 2200, 190, 750),
    ELECTRIC(7, "electric", 38, 3800, 250, 1150),
    ATOMIC(8, "atomic", 36, 6000, 320, 1700),
    INFORMATION(9, "information", 34, 9000, 400, 2500),
    SPACE(10, "space", 34, 13000, 500, 3500);

    private static final Map<String, TechEra> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(TechEra::id, Function.identity()));

    public static final Codec<TechEra> CODEC = Codec.STRING.comapFlatMap(
            raw -> {
                String normalized = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
                TechEra era = BY_ID.get(normalized);
                return era != null
                        ? DataResult.success(era)
                        : DataResult.error(() -> "Unknown TechEra: " + raw);
            },
            TechEra::id
    );

    private final int index;
    private final String id;
    private final int targetCount;
    private final int averageRp;
    private final int referenceThreshold;
    private final int typicalRpPerDay;

    TechEra(int index, String id, int targetCount, int averageRp, int referenceThreshold, int typicalRpPerDay) {
        this.index = index;
        this.id = id;
        this.targetCount = targetCount;
        this.averageRp = averageRp;
        this.referenceThreshold = referenceThreshold;
        this.typicalRpPerDay = typicalRpPerDay;
    }

    public int index() {
        return index;
    }

    public String id() {
        return id;
    }

    public Identifier identifier() {
        return Identifier.fromNamespaceAndPath("tctechnology", id);
    }

    public String code() {
        return "E" + index;
    }

    public int targetCount() {
        return targetCount;
    }

    public int averageRp() {
        return averageRp;
    }

    public int minAllowedRp() {
        return Math.max(1, averageRp / 2);
    }

    public int maxAllowedRp() {
        return averageRp * 2;
    }

    public int referenceThreshold() {
        return referenceThreshold;
    }

    public int typicalRpPerDay() {
        return typicalRpPerDay;
    }

    public Optional<TechEra> next() {
        int nextIdx = index + 1;
        return nextIdx < values().length ? Optional.of(values()[nextIdx]) : Optional.empty();
    }

    public static Optional<TechEra> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return Optional.ofNullable(BY_ID.get(normalized));
    }

    public static TechEra byIndex(int idx) {
        TechEra[] all = values();
        return all[Math.clamp(idx, 0, all.length - 1)];
    }
}
