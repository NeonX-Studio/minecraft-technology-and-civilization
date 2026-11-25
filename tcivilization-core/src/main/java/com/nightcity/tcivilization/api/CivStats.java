package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.TCivilization;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Mutable/serializable counter dictionary stored per civilization (03 §5.1.1).
 */
public final class CivStats implements CivStatsView {
    public static final Identifier REGIONS_VISITED = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "regions_visited");
    public static final Identifier PLAY_TICKS = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "play_ticks");
    public static final Identifier POPULATION = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "population");
    public static final Identifier MEMORY_ENTRIES = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "memory_entries");
    public static final Identifier FEATS = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "feats");
    public static final Identifier PEAK_PLAYERS = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "peak_players");

    public static final Codec<CivStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("version", 1L).forGetter(CivStats::version),
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
                    .optionalFieldOf("values", Map.of())
                    .forGetter(CivStats::asMap)
    ).apply(instance, CivStats::new));

    private long version;
    private final Map<Identifier, Long> values = new LinkedHashMap<>();
    private Runnable dirtyCallback = () -> {};

    public CivStats() {
        this(1L, Map.of());
    }

    public CivStats(long version, Map<Identifier, Long> initial) {
        this.version = Math.max(1L, version);
        if (initial != null) {
            this.values.putAll(initial);
        }
    }

    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback == null ? () -> {} : callback;
    }

    @Override
    public synchronized long version() {
        return version;
    }

    public synchronized long bumpVersion() {
        version++;
        dirtyCallback.run();
        return version;
    }

    @Override
    public synchronized long get(Identifier statId) {
        return values.getOrDefault(statId, 0L);
    }

    @Override
    public synchronized void set(Identifier statId, long value) {
        Objects.requireNonNull(statId, "statId");
        values.put(statId, value);
        dirtyCallback.run();
    }

    @Override
    public synchronized long add(Identifier statId, long delta) {
        Objects.requireNonNull(statId, "statId");
        long next = values.getOrDefault(statId, 0L) + delta;
        values.put(statId, next);
        dirtyCallback.run();
        return next;
    }

    @Override
    public synchronized Map<Identifier, Long> asMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public synchronized CivStats copy() {
        return new CivStats(version, values);
    }
}
