package com.nightcity.tcivilization.region;

import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.RegionPressureContributor;
import com.nightcity.tcivilization.api.event.RegionLoadPressureEvent;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

/**
 * Computes normalized region load pressure and emits RegionLoadPressureEvent with 0.05 hysteresis (03 §5.2.4).
 */
public final class RegionPressureTracker {
    public static final Identifier POP_DENSITY_ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "population_density");
    public static final Identifier BUILD_DENSITY_ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "build_density");

    private final Map<Identifier, RegionPressureContributor> contributors = new LinkedHashMap<>();
    private final Map<RegionCell, RegionLoadPressureEvent.Level> levels = new ConcurrentHashMap<>();
    private final TcEventBus eventBus;

    public RegionPressureTracker(TcEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        registerBuiltinContributors();
    }

    private void registerBuiltinContributors() {
        registerContributor(new RegionPressureContributor() {
            @Override
            public Identifier id() {
                return POP_DENSITY_ID;
            }

            @Override
            public float weight() {
                return 0.40f;
            }

            @Override
            public float evaluate(RegionHandle region) {
                CivRegionData civ = region.layer(CivRegionLayer.INSTANCE);
                return Math.min(1.0f, civ.personCount() / 200.0f);
            }
        });
        registerContributor(new RegionPressureContributor() {
            @Override
            public Identifier id() {
                return BUILD_DENSITY_ID;
            }

            @Override
            public float weight() {
                return 0.20f;
            }

            @Override
            public float evaluate(RegionHandle region) {
                CivRegionData civ = region.layer(CivRegionLayer.INSTANCE);
                return Math.min(1.0f, civ.placedBlocks() / 250_000.0f);
            }
        });
    }

    public synchronized void registerContributor(RegionPressureContributor contributor) {
        Objects.requireNonNull(contributor, "contributor");
        if (contributor.weight() <= 0.0f) {
            throw new IllegalArgumentException("Contributor weight must be positive: " + contributor.id());
        }
        contributors.put(contributor.id(), contributor);
    }

    public synchronized void clearContributors() {
        contributors.clear();
    }

    public synchronized float computePressure(RegionHandle region) {
        if (contributors.isEmpty()) {
            return 0.0f;
        }
        float weightedSum = 0.0f;
        float totalWeight = 0.0f;
        for (RegionPressureContributor contributor : contributors.values()) {
            float w = contributor.weight();
            float component = Math.clamp(contributor.evaluate(region), 0.0f, 1.0f);
            weightedSum += w * component;
            totalWeight += w;
        }
        float raw = totalWeight > 1.0f ? (weightedSum / totalWeight) : weightedSum;
        return Math.clamp(raw, 0.0f, 1.0f);
    }

    public float recalculateAndUpdate(RegionHandle region) {
        float computed = computePressure(region);
        region.update(CivRegionLayer.INSTANCE, data -> data.withPressure(computed));
        applyPressure(region.cell(), computed);
        return computed;
    }

    public RegionLoadPressureEvent.Level applyPressure(RegionCell cell, float pressure) {
        float clamped = Math.clamp(pressure, 0.0f, 1.0f);
        RegionLoadPressureEvent.Level prev = levels.getOrDefault(cell, RegionLoadPressureEvent.Level.NONE);
        RegionLoadPressureEvent.Level next = evaluateWithHysteresis(prev, clamped);
        if (next != prev) {
            if (next == RegionLoadPressureEvent.Level.NONE) {
                levels.remove(cell);
            } else {
                levels.put(cell, next);
            }
            eventBus.enqueue(new RegionLoadPressureEvent(cell, prev, next, clamped));
        }
        return next;
    }

    public RegionLoadPressureEvent.Level currentLevel(RegionCell cell) {
        return levels.getOrDefault(cell, RegionLoadPressureEvent.Level.NONE);
    }

    public static RegionLoadPressureEvent.Level evaluateWithHysteresis(
            RegionLoadPressureEvent.Level current, float pressure) {
        RegionLoadPressureEvent.Level[] all = RegionLoadPressureEvent.Level.values();
        RegionLoadPressureEvent.Level upward = RegionLoadPressureEvent.Level.NONE;
        for (int i = all.length - 1; i >= 1; i--) {
            if (pressure >= all[i].threshold()) {
                upward = all[i];
                break;
            }
        }
        if (upward.ordinal() > current.ordinal()) {
            return upward;
        }

        RegionLoadPressureEvent.Level retained = current;
        while (retained.ordinal() > 0 && pressure < retained.downwardThreshold()) {
            retained = all[retained.ordinal() - 1];
        }
        return retained;
    }
}
