package com.nightcity.tcivilization.sim;

import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.PersonRecord;
import com.nightcity.tcivilization.api.PersonState;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.SimTier;
import com.nightcity.tcivilization.api.event.SimulationStepEvent;
import com.nightcity.tcivilization.person.PersonEntity;
import com.nightcity.tcivilization.person.PersonManager;
import com.nightcity.tcivilization.person.PersonNeeds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.minecraft.core.BlockPos;

/**
 * L0-L3 simulation scheduler, MAX_EMBODIED_NPC=500 budget selector with anti-flap hysteresis,
 * catch-up step calculator, and TC-Sim worker command queue (03 §5.8).
 */
public final class SimulationScheduler implements AutoCloseable {
    public static final int MAX_EMBODIED_NPC = 500;
    public static final double DEFAULT_EMBODY_RADIUS = 128.0D;
    public static final int DEFAULT_MAX_SWITCHES_PER_PASS = 32;
    public static final float EMBODIED_RETENTION_FACTOR = 0.85f;
    public static final long ANTI_FLAP_COOLDOWN_TICKS = 40L;
    public static final int DEFAULT_CATCH_UP_CAP_DAYS = 3;
    public static final int DEFAULT_CATCH_UP_STEPS_PER_TICK = 16;
    public static final double DEFAULT_APPLY_BUDGET_MS = 0.6D;

    public static final long L1_INTERVAL_TICKS = 20L;
    public static final long L2_INTERVAL_TICKS = 1_000L;
    public static final long L3_INTERVAL_TICKS = 24_000L;

    @FunctionalInterface
    public interface SimCommand {
        void apply();
    }

    public record CatchUpPlan(long elapsedTicks, long cappedTicks, int totalSteps, int immediateSteps, int queuedSteps) {}

    private final PersonManager personManager;
    private final TcEventBus eventBus;
    private final ExecutorService simExecutor;
    private final Queue<SimCommand> applyQueue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Long> lastSwitchTick = new HashMap<>();

    private int maxEmbodiedNpc = MAX_EMBODIED_NPC;
    private double embodyRadius = DEFAULT_EMBODY_RADIUS;
    private int maxSwitchesPerPass = DEFAULT_MAX_SWITCHES_PER_PASS;
    private int catchUpCapDays = DEFAULT_CATCH_UP_CAP_DAYS;
    private int catchUpStepsPerTick = DEFAULT_CATCH_UP_STEPS_PER_TICK;

    public SimulationScheduler(PersonManager personManager, TcEventBus eventBus) {
        this.personManager = Objects.requireNonNull(personManager, "personManager");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        int workers = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2));
        AtomicInteger idx = new AtomicInteger(1);
        this.simExecutor = Executors.newFixedThreadPool(workers, runnable -> {
            Thread t = new Thread(runnable, "TC-Sim-" + idx.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    public void setMaxEmbodiedNpc(int maxEmbodiedNpc) {
        this.maxEmbodiedNpc = Math.clamp(maxEmbodiedNpc, 1, 1000);
    }

    public int maxEmbodiedNpc() {
        return maxEmbodiedNpc;
    }

    public void setMaxSwitchesPerPass(int maxSwitches) {
        this.maxSwitchesPerPass = Math.clamp(maxSwitches, 1, 128);
    }

    public int pendingCommandCount() {
        return applyQueue.size();
    }

    public CompletableFuture<Void> submitBackgroundStep(Runnable snapshotComputation) {
        return CompletableFuture.runAsync(snapshotComputation, simExecutor);
    }

    public void enqueueCommand(SimCommand command) {
        applyQueue.add(Objects.requireNonNull(command, "command"));
    }

    public int applyPendingCommands(double budgetMs) {
        long budgetNanos = Math.max(50_000L, (long) (budgetMs * 1_000_000.0D));
        long deadline = System.nanoTime() + budgetNanos;
        int applied = 0;
        while (applied == 0 || System.nanoTime() < deadline) {
            SimCommand cmd = applyQueue.poll();
            if (cmd == null) {
                break;
            }
            cmd.apply();
            applied++;
        }
        return applied;
    }

    public CatchUpPlan computeCatchUp(long nowTick, long lastSimulatedTick) {
        long elapsed = Math.max(0L, nowTick - lastSimulatedTick);
        long capTicks = (long) catchUpCapDays * CivCalendar.DAY_TICKS;
        long capped = Math.min(elapsed, capTicks);
        int totalSteps = (int) ((capped + L2_INTERVAL_TICKS - 1L) / L2_INTERVAL_TICKS);
        int immediate = Math.min(totalSteps, catchUpStepsPerTick);
        int queued = Math.max(0, totalSteps - immediate);
        return new CatchUpPlan(elapsed, capped, totalSteps, immediate, queued);
    }

    public synchronized int rebalanceEmbodiment(
            long currentTick,
            List<BlockPos> playerPositions,
            Function<PersonRecord, PersonEntity> entitySpawner
    ) {
        record ScoredCandidate(PersonRecord record, double distance, double score) {}
        List<ScoredCandidate> candidates = new ArrayList<>();

        for (PersonRecord rec : personManager.all()) {
            if (rec.state() != PersonState.EMBODIED && rec.state() != PersonState.PROXY) {
                continue;
            }
            double nearestDist = nearestDistance(rec.lastPos().pos(), playerPositions);
            float factor = personManager.effectiveImportanceFactor(rec, currentTick);
            double score = nearestDist * factor;
            if (rec.state() == PersonState.EMBODIED) {
                score *= EMBODIED_RETENTION_FACTOR;
            }
            candidates.add(new ScoredCandidate(rec, nearestDist, score));
        }

        candidates.sort(Comparator.comparingDouble(ScoredCandidate::score));

        Set<UUID> desiredEmbodied = new HashSet<>();
        for (ScoredCandidate sc : candidates) {
            boolean inDialogue = personManager.effectiveImportanceFactor(sc.record(), currentTick) == 0.0f;
            if (inDialogue || (sc.distance() <= embodyRadius && desiredEmbodied.size() < maxEmbodiedNpc)) {
                desiredEmbodied.add(sc.record().id());
            }
        }

        int switches = 0;
        for (ScoredCandidate sc : candidates) {
            if (switches >= maxSwitchesPerPass) {
                break;
            }
            PersonRecord rec = sc.record();
            if (rec.state() == PersonState.EMBODIED && !desiredEmbodied.contains(rec.id())) {
                if (canSwitch(rec.id(), currentTick) && personManager.transitionFromEmbodied(rec.id(), PersonState.PROXY)) {
                    lastSwitchTick.put(rec.id(), currentTick);
                    switches++;
                }
            }
        }

        int currentEmbodied = personManager.embodiedCount();
        for (ScoredCandidate sc : candidates) {
            if (switches >= maxSwitchesPerPass || currentEmbodied >= maxEmbodiedNpc) {
                break;
            }
            PersonRecord rec = sc.record();
            if (rec.state() == PersonState.PROXY && desiredEmbodied.contains(rec.id())) {
                if (!canSwitch(rec.id(), currentTick)) {
                    continue;
                }
                PersonEntity ent = entitySpawner == null ? null : entitySpawner.apply(rec);
                if (personManager.transitionToEmbodied(rec.id(), ent)) {
                    lastSwitchTick.put(rec.id(), currentTick);
                    switches++;
                    currentEmbodied++;
                }
            }
        }
        return switches;
    }

    public void tickServer(long currentTick, List<BlockPos> playerPositions, Function<PersonRecord, PersonEntity> spawner) {
        applyPendingCommands(DEFAULT_APPLY_BUDGET_MS);
        if (currentTick % L1_INTERVAL_TICKS == 0L) {
            rebalanceEmbodiment(currentTick, playerPositions, spawner);
            runL1ProxyDecay();
            eventBus.enqueue(new SimulationStepEvent(SimTier.L1, null));
        }
        if (currentTick % L2_INTERVAL_TICKS == 0L) {
            eventBus.enqueue(new SimulationStepEvent(SimTier.L2, (RegionCell) null));
        }
        if (currentTick > 0L && currentTick % L3_INTERVAL_TICKS == 0L) {
            eventBus.enqueue(new SimulationStepEvent(SimTier.L3, null));
        }
    }

    private void runL1ProxyDecay() {
        float fraction = (float) L1_INTERVAL_TICKS / (float) CivCalendar.DAY_TICKS;
        for (PersonRecord rec : personManager.all()) {
            if (rec.state() == PersonState.PROXY) {
                personManager.component(rec.id(), PersonNeeds.TYPE).ifPresent(needs ->
                        personManager.setComponent(rec.id(), PersonNeeds.TYPE, needs.decayFractionOfDay(fraction, false, true)));
            }
        }
    }

    private boolean canSwitch(UUID personId, long currentTick) {
        Long last = lastSwitchTick.get(personId);
        return last == null || (currentTick - last) >= ANTI_FLAP_COOLDOWN_TICKS;
    }

    private static double nearestDistance(BlockPos target, List<BlockPos> players) {
        if (players == null || players.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double best = Double.POSITIVE_INFINITY;
        for (BlockPos p : players) {
            double dx = target.getX() - p.getX();
            double dy = target.getY() - p.getY();
            double dz = target.getZ() - p.getZ();
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < best) {
                best = d;
            }
        }
        return best;
    }

    @Override
    public void close() {
        simExecutor.shutdown();
    }
}
