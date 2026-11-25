package com.nightcity.tcivilization;

import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.ImportanceFlag;
import com.nightcity.tcivilization.api.PersonName;
import com.nightcity.tcivilization.api.PersonRecord;
import com.nightcity.tcivilization.api.PersonSpec;
import com.nightcity.tcivilization.api.PersonState;
import com.nightcity.tcivilization.person.HierarchicalPathfinder;
import com.nightcity.tcivilization.person.PersonComponentRegistry;
import com.nightcity.tcivilization.person.PersonManager;
import com.nightcity.tcivilization.person.PersonNeeds;
import com.nightcity.tcivilization.person.PersonSchedule;
import com.nightcity.tcivilization.sim.SimulationScheduler;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonAndSchedulerTest {
    @Test
    void enforcesMax500EmbodiedNpcCapDialoguePriorityAndAntiFlapCooldown() {
        com.nightcity.tcivilization.api.TcThreads.bindMainThread(Thread.currentThread());
        try {
        AtomicLong nowTick = new AtomicLong(100L);
        TcEventBus bus = new TcEventBus(e -> {});
        PersonManager manager = new PersonManager(new PersonComponentRegistry(), bus, nowTick::get);

        try (SimulationScheduler scheduler = new SimulationScheduler(manager, bus)) {
            scheduler.setMaxSwitchesPerPass(128);
            BlockPos origin = new BlockPos(0, 64, 0);

            UUID dialogueNpc = null;
            for (int i = 0; i < 600; i++) {
                BlockPos pos = new BlockPos(10 + (i % 60), 64, 10 + (i / 60));
                PersonRecord created = manager.create(new PersonSpec(
                        Civilization.PLAYER_CIV_ID,
                        Optional.empty(),
                        new PersonName("Npc" + i, "Clan", Optional.empty()),
                        1,
                        PersonSpec.DEFAULT_CULTURE,
                        i,
                        GlobalPos.of(Level.OVERWORLD, pos),
                        EnumSet.noneOf(ImportanceFlag.class)
                ));
                manager.setChunkLoadedState(created.id(), true);
                if (i == 599) {
                    dialogueNpc = created.id();
                }
            }

            // Boost the farthest NPC (#599) as active dialogue (importanceFactor = 0)
            manager.requestEmbody(dialogueNpc, 0.0f, 400);

            // Run several passes past maxSwitchesPerPass=128 until steady state
            for (int pass = 0; pass < 6; pass++) {
                scheduler.rebalanceEmbodiment(nowTick.get(), List.of(origin), rec -> null);
                nowTick.addAndGet(50L);
            }

            assertEquals(SimulationScheduler.MAX_EMBODIED_NPC, manager.embodiedCount(),
                    "Embodied NPC count must never exceed MAX_EMBODIED_NPC=500");
            assertEquals(PersonState.EMBODIED, manager.get(dialogueNpc).orElseThrow().state(),
                    "Dialogue NPC with importanceFactor=0 must always be embodied");

            // Anti-flap check: moving player far away 10 ticks after last switch (< 40 ticks cooldown) prevents immediate demotion
            final UUID pinnedDialogueNpc = dialogueNpc;
            UUID sampleEmbodied = manager.all().stream()
                    .filter(r -> r.state() == PersonState.EMBODIED && !r.id().equals(pinnedDialogueNpc))
                    .findFirst().orElseThrow().id();
            // Force a fresh transition at tick 1000
            manager.transitionFromEmbodied(sampleEmbodied, PersonState.PROXY);
            nowTick.set(1_000L);
            scheduler.rebalanceEmbodiment(1_000L, List.of(origin), rec -> null);
            assertEquals(PersonState.EMBODIED, manager.get(sampleEmbodied).orElseThrow().state());

            // Move player 5,000 blocks away at tick 1,010 (only 10 ticks elapsed < 40 cooldown)
            nowTick.set(1_010L);
            scheduler.rebalanceEmbodiment(1_010L, List.of(new BlockPos(5_000, 64, 5_000)), rec -> null);
            assertEquals(PersonState.EMBODIED, manager.get(sampleEmbodied).orElseThrow().state(),
                    "40-tick anti-flap cooldown must block premature demotion");
        }
        } finally {
            com.nightcity.tcivilization.api.TcThreads.bindMainThread(null);
        }
    }

    @Test
    void computesBoundedCatchUpStepsAndAppliesBackgroundWorkerCommands() throws Exception {
        TcEventBus bus = new TcEventBus(java.util.Objects::requireNonNull);
        PersonManager manager = new PersonManager(new PersonComponentRegistry(), bus, () -> 0L);
        try (SimulationScheduler scheduler = new SimulationScheduler(manager, bus)) {
            // 10 days offline -> capped at 3 days = 72,000 ticks -> 72 L2 steps -> 16 immediate + 56 queued
            SimulationScheduler.CatchUpPlan plan = scheduler.computeCatchUp(10L * 24_000L, 0L);
            assertEquals(72_000L, plan.cappedTicks());
            assertEquals(72, plan.totalSteps());
            assertEquals(16, plan.immediateSteps());
            assertEquals(56, plan.queuedSteps());

            AtomicInteger counter = new AtomicInteger();
            scheduler.submitBackgroundStep(() -> scheduler.enqueueCommand(counter::incrementAndGet)).join();
            assertEquals(1, scheduler.applyPendingCommands(0.6D));
            assertEquals(1, counter.get());
        }
    }

    @Test
    void evaluatesNeedsMoodEfficiencyScheduleAndHierarchicalPathfinding() throws Exception {
        PersonNeeds full = PersonNeeds.full();
        assertEquals(100.0f, full.mood(), 1e-4f);
        assertEquals(1.0f, full.workEfficiency(), 1e-4f);

        PersonNeeds exhausted = new PersonNeeds(10.0f, 10.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(0.56f, exhausted.workEfficiency(), 1e-4f);
        assertTrue(exhausted.workEfficiency() >= 0.4f);

        PersonSchedule schedule = PersonSchedule.standard();
        assertEquals(PersonSchedule.MEAL, schedule.activityAt(0L));
        assertEquals(PersonSchedule.WORK, schedule.activityAt(2_500L));
        assertEquals(PersonSchedule.SLEEP, schedule.activityAt(20_000L));
        PersonSchedule overridden = schedule.withOverride(PersonSchedule.SHELTER, 5_000L);
        assertEquals(PersonSchedule.SHELTER, overridden.activityAt(2_500L));
        assertEquals(PersonSchedule.SLEEP, overridden.activityAt(20_000L));

        try (HierarchicalPathfinder pf = new HierarchicalPathfinder(2)) {
            BlockPos a = new BlockPos(0, 64, 0);
            assertEquals(HierarchicalPathfinder.PathTier.LOCAL, HierarchicalPathfinder.classifyDistance("minecraft:overworld", a, new BlockPos(32, 64, 0)));
            assertEquals(HierarchicalPathfinder.PathTier.MID_RANGE, HierarchicalPathfinder.classifyDistance("minecraft:overworld", a, new BlockPos(200, 64, 0)));
            assertEquals(HierarchicalPathfinder.PathTier.LONG_RANGE, HierarchicalPathfinder.classifyDistance("minecraft:overworld", a, new BlockPos(2_048, 64, 0)));

            HierarchicalPathfinder.PathPlan longPlan = pf.planAsync("minecraft:overworld", new BlockPos(81_800, 64, 0), new BlockPos(-81_800, 64, 0), true).get();
            // Across the X=±81,920 wrap seam, distance is only 240 blocks -> MID_RANGE!
            assertEquals(HierarchicalPathfinder.PathTier.MID_RANGE, longPlan.tier());

            HierarchicalPathfinder.PathPlan multiRegion = pf.planAsync("minecraft:overworld", a, new BlockPos(1_600, 64, 512), true).get();
            assertEquals(HierarchicalPathfinder.PathTier.LONG_RANGE, multiRegion.tier());
            assertTrue(multiRegion.regionWaypoints().size() >= 3);
        }
    }

    @Test
    void killRequiresExplicitNonNullCause() {
        com.nightcity.tcivilization.api.TcThreads.bindMainThread(Thread.currentThread());
        try {
            PersonManager manager = new PersonManager(new PersonComponentRegistry(), new TcEventBus(java.util.Objects::requireNonNull), () -> 0L);
        PersonRecord rec = manager.create(new PersonSpec(
                Civilization.PLAYER_CIV_ID, Optional.empty(),
                new PersonName("Ada", "Stone", Optional.empty()), 1,
                PersonSpec.DEFAULT_CULTURE, 42L,
                GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO),
                EnumSet.of(ImportanceFlag.NAMED)
        ));
            assertThrows(NullPointerException.class, () -> manager.kill(rec.id(), null));
            manager.kill(rec.id(), Identifier.fromNamespaceAndPath("tcivilization", "starvation"));
            assertEquals(PersonState.DEAD, manager.get(rec.id()).orElseThrow().state());
        } finally {
            com.nightcity.tcivilization.api.TcThreads.bindMainThread(null);
        }
    }
}
