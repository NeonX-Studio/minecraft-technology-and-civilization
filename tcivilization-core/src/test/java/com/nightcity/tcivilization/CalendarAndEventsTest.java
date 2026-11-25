package com.nightcity.tcivilization;

import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.ProficiencyBridge;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionGridRegistry;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.api.event.RegionLoadPressureEvent;
import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import com.nightcity.tcivilization.region.RegionManager;
import com.nightcity.tcivilization.region.RegionPressureTracker;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CalendarAndEventsTest {
    @TempDir
    Path tempDir;

    private static final class DummyEvent extends TcEvent {
        final int depth;

        DummyEvent(int depth) {
            this.depth = depth;
        }
    }

    @Test
    void convertsDayTimeTo48DayYearAndFourSeasonsDeterministically() {
        CivCalendar.CalendarDate start = CivCalendar.fromDayTime(0L);
        assertEquals(1, start.year());
        assertEquals(CivCalendar.Season.SPRING, start.season());
        assertEquals(1, start.dayOfSeason());

        // Day index 12 (tick 12 * 24000) -> Summer Day 1
        CivCalendar.CalendarDate summer1 = CivCalendar.fromDayTime(12L * 24_000L);
        assertEquals(1, summer1.year());
        assertEquals(CivCalendar.Season.SUMMER, summer1.season());
        assertEquals(1, summer1.dayOfSeason());

        // Last tick of Year 1 (48 * 24000 - 1 = 1,151,999) -> Year 1 Winter Day 12
        CivCalendar.CalendarDate endYear1 = CivCalendar.fromDayTime(48L * 24_000L - 1L);
        assertEquals(1, endYear1.year());
        assertEquals(CivCalendar.Season.WINTER, endYear1.season());
        assertEquals(12, endYear1.dayOfSeason());

        // Tick 1,152,000 -> Year 2 Spring Day 1
        CivCalendar.CalendarDate year2 = CivCalendar.fromDayTime(48L * 24_000L);
        assertEquals(2, year2.year());
        assertEquals(CivCalendar.Season.SPRING, year2.season());
        assertEquals(1, year2.dayOfSeason());
    }

    @Test
    void enforcesEventTickBudgetMonotonicSequenceAndNestingDepth() {
        List<Long> seenSeqs = new ArrayList<>();
        TcEventBus bus = new TcEventBus(ev -> {
            if (ev instanceof DummyEvent de) {
                seenSeqs.add(de.sequence());
            }
        });

        for (int i = 0; i < 300; i++) {
            bus.enqueue(new DummyEvent(0));
        }
        int firstFlush = bus.flushTick();
        assertEquals(TcEventBus.MAX_EVENTS_PER_TICK, firstFlush);
        assertEquals(44, bus.pendingCount());

        int secondFlush = bus.flushTick();
        assertEquals(44, secondFlush);
        assertEquals(0, bus.pendingCount());
        assertEquals(300, seenSeqs.size());
        for (int i = 1; i < seenSeqs.size(); i++) {
            assertTrue(seenSeqs.get(i) > seenSeqs.get(i - 1));
        }
    }

    @Test
    void appliesFivePercentHysteresisToRegionLoadPressureThresholds() {
        TcThreads.bindMainThread(Thread.currentThread());
        try {
            RegionLoadPressureEvent.Level level = RegionPressureTracker.evaluateWithHysteresis(
                    RegionLoadPressureEvent.Level.MEDIUM, 0.76f);
            assertEquals(RegionLoadPressureEvent.Level.HIGH, level);
            assertEquals(RegionLoadPressureEvent.Level.HIGH,
                    RegionPressureTracker.evaluateWithHysteresis(level, 0.72f));
            assertEquals(RegionLoadPressureEvent.Level.MEDIUM,
                    RegionPressureTracker.evaluateWithHysteresis(level, 0.69f));

            List<RegionLoadPressureEvent> fired = new ArrayList<>();
            TcEventBus bus = new TcEventBus(ev -> {
                if (ev instanceof RegionLoadPressureEvent rlpe) {
                    fired.add(rlpe);
                }
            });

            RegionLayerRegistry layers = new RegionLayerRegistry();
            layers.register(com.nightcity.tcivilization.region.CivRegionLayer.INSTANCE);
            AtomicReference<Float> dynamicLoad = new AtomicReference<>(0.76f);
            try (RegionManager mgr = new RegionManager(tempDir, 1, 8, RegionGridRegistry.defaults(), layers, bus, () -> 0L)) {
                mgr.pressureTracker().clearContributors();
                mgr.pressureTracker().registerContributor(new com.nightcity.tcivilization.api.RegionPressureContributor() {
                    @Override
                    public Identifier id() {
                        return Identifier.fromNamespaceAndPath("test", "load");
                    }

                    @Override
                    public float weight() {
                        return 1.0f;
                    }

                    @Override
                    public float evaluate(RegionHandle region) {
                        return dynamicLoad.get();
                    }
                });

                RegionHandle h = mgr.getByDimensionId("minecraft:overworld", 2, 2).orElseThrow();
                mgr.pressureTracker().recalculateAndUpdate(h);
                bus.flushTick();
                assertEquals(1, fired.size());

                dynamicLoad.set(0.73f);
                mgr.pressureTracker().recalculateAndUpdate(h);
                bus.flushTick();
                assertEquals(1, fired.size(), "Hysteresis must suppress repeated event within 0.05 band");
            }
        } finally {
            TcThreads.bindMainThread(null);
        }
    }

    @Test
    void proficiencyBridgeValidates22SkillsAndSupportsZeroFallback() {
        ProficiencyBridge.clearSinkForTests();
        assertEquals(22, ProficiencyBridge.VALID_SKILLS.size());
        assertTrue(ProficiencyBridge.isValidSkillId("smithing"));
        assertTrue(ProficiencyBridge.isValidSkillId("tcskills:smithing"));
        assertFalse(ProficiencyBridge.isValidSkillId("nonexistent_skill"));
        assertEquals(1.0D, ProficiencyBridge.multiplier(null, "smithing", Identifier.fromNamespaceAndPath("tcivilization", "speed")));

        boolean prevStrict = TcThreads.isStrictMode();
        try {
            TcThreads.setStrictMode(true);
            assertThrows(IllegalArgumentException.class, () ->
                    ProficiencyBridge.multiplier(null, "nonexistent_skill", Identifier.fromNamespaceAndPath("tcivilization", "speed")));
        } finally {
            TcThreads.setStrictMode(prevStrict);
        }
    }

    @Test
    void strictModeDetectsOffThreadMainThreadAssertion() throws Exception {
        boolean prevStrict = TcThreads.isStrictMode();
        try {
            TcThreads.setStrictMode(true);
            TcThreads.bindMainThread(Thread.currentThread());
            assertDoesNotThrow(() -> TcThreads.assertMain());

            AtomicReference<Throwable> workerError = new AtomicReference<>();
            Thread worker = new Thread(() -> {
                try {
                    TcThreads.assertMain();
                } catch (Throwable t) {
                    workerError.set(t);
                }
            }, "Test-OffThread");
            worker.start();
            worker.join();
            assertInstanceOf(IllegalStateException.class, workerError.get());
        } finally {
            TcThreads.bindMainThread(null);
            TcThreads.setStrictMode(prevStrict);
        }
    }
}
