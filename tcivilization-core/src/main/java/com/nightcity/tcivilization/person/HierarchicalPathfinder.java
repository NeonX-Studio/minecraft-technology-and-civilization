package com.nightcity.tcivilization.person;

import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.RegionCell;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleBiFunction;
import net.minecraft.core.BlockPos;

/**
 * Three-tier hierarchical pathfinding skeleton (03 §5.7.8):
 * LOCAL (<=64), MID_RANGE (64..512), LONG_RANGE (>512 across region graph on TC-Path 2 threads).
 */
public final class HierarchicalPathfinder implements AutoCloseable {
    public static final double LOCAL_MAX_BLOCKS = 64.0D;
    public static final double MID_RANGE_MAX_BLOCKS = 512.0D;

    public enum PathTier {
        LOCAL,
        MID_RANGE,
        LONG_RANGE
    }

    public record PathPlan(PathTier tier, List<RegionCell> regionWaypoints, BlockPos nextPortal) {}

    private final ExecutorService pathExecutor;
    private volatile ToDoubleBiFunction<RegionCell, RegionCell> edgeCostProvider = (from, to) -> 1.0D;

    public HierarchicalPathfinder(int threads) {
        int count = Math.clamp(threads, 1, 4);
        AtomicInteger idx = new AtomicInteger(1);
        this.pathExecutor = Executors.newFixedThreadPool(count, runnable -> {
            Thread t = new Thread(runnable, "TC-Path-" + idx.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    public void setEdgeCostProvider(ToDoubleBiFunction<RegionCell, RegionCell> provider) {
        this.edgeCostProvider = Objects.requireNonNull(provider, "provider");
    }

    public static PathTier classifyDistance(String dimension, BlockPos from, BlockPos to) {
        double dx = "minecraft:overworld".equals(dimension)
                ? RegionCoordinates.wrapX(to.getX() - from.getX())
                : (to.getX() - from.getX());
        double dz = to.getZ() - from.getZ();
        double dist = Math.hypot(dx, dz);
        if (dist <= LOCAL_MAX_BLOCKS) {
            return PathTier.LOCAL;
        }
        if (dist <= MID_RANGE_MAX_BLOCKS) {
            return PathTier.MID_RANGE;
        }
        return PathTier.LONG_RANGE;
    }

    public CompletableFuture<PathPlan> planAsync(String dimension, BlockPos start, BlockPos goal, boolean wrapX) {
        PathTier tier = classifyDistance(dimension, start, goal);
        if (tier == PathTier.LOCAL) {
            return CompletableFuture.completedFuture(new PathPlan(PathTier.LOCAL, List.of(), goal));
        }
        if (tier == PathTier.MID_RANGE) {
            double dx = wrapX ? RegionCoordinates.wrapX(goal.getX() - start.getX()) : (goal.getX() - start.getX());
            double dz = goal.getZ() - start.getZ();
            double dist = Math.max(1.0D, Math.hypot(dx, dz));
            double scale = LOCAL_MAX_BLOCKS / dist;
            BlockPos portal = new BlockPos(
                    start.getX() + (int) Math.round(dx * scale),
                    start.getY(),
                    start.getZ() + (int) Math.round(dz * scale)
            );
            return CompletableFuture.completedFuture(new PathPlan(PathTier.MID_RANGE, List.of(), portal));
        }
        return CompletableFuture.supplyAsync(() -> computeRegionPath(dimension, start, goal, wrapX), pathExecutor);
    }

    private PathPlan computeRegionPath(String dimension, BlockPos start, BlockPos goal, boolean wrapX) {
        int startRx = wrapX ? RegionCoordinates.regionX(start.getX()) : Math.floorDiv(start.getX(), 512);
        int startRz = Math.floorDiv(start.getZ(), 512);
        int goalRx = wrapX ? RegionCoordinates.regionX(goal.getX()) : Math.floorDiv(goal.getX(), 512);
        int goalRz = Math.floorDiv(goal.getZ(), 512);

        RegionCell startCell = new RegionCell(dimension, startRx, startRz);
        RegionCell goalCell = new RegionCell(dimension, goalRx, goalRz);
        if (startCell.equals(goalCell)) {
            return new PathPlan(PathTier.LONG_RANGE, List.of(startCell), goal);
        }

        record Node(RegionCell cell, double g, double f) {}
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Map<RegionCell, Double> bestG = new HashMap<>();
        Map<RegionCell, RegionCell> parent = new HashMap<>();

        open.add(new Node(startCell, 0.0D, heuristic(startCell, goalCell, wrapX)));
        bestG.put(startCell, 0.0D);

        int expansions = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!open.isEmpty() && expansions < 4096) {
            Node cur = open.poll();
            expansions++;
            if (cur.cell().equals(goalCell)) {
                break;
            }
            if (cur.g() > bestG.getOrDefault(cur.cell(), Double.POSITIVE_INFINITY)) {
                continue;
            }
            for (int[] d : dirs) {
                int nx = cur.cell().x() + d[0];
                if (wrapX) {
                    nx = Math.floorMod(nx + 160, 320) - 160;
                }
                int nz = cur.cell().z() + d[1];
                if (nz < -80 || nz >= 80) {
                    continue;
                }
                RegionCell next = new RegionCell(dimension, nx, nz);
                double step = Math.max(0.05D, edgeCostProvider.applyAsDouble(cur.cell(), next));
                double ng = cur.g() + step;
                if (ng < bestG.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    bestG.put(next, ng);
                    parent.put(next, cur.cell());
                    open.add(new Node(next, ng, ng + heuristic(next, goalCell, wrapX)));
                }
            }
        }

        List<RegionCell> route = new ArrayList<>();
        RegionCell cursor = goalCell;
        if (parent.containsKey(goalCell)) {
            while (cursor != null) {
                route.addFirst(cursor);
                cursor = parent.get(cursor);
            }
        } else {
            route.add(startCell);
            route.add(goalCell);
        }
        RegionCell nextStep = route.size() > 1 ? route.get(1) : goalCell;
        BlockPos entry = new BlockPos(nextStep.x() * 512 + 256, start.getY(), nextStep.z() * 512 + 256);
        return new PathPlan(PathTier.LONG_RANGE, List.copyOf(route), entry);
    }

    private static double heuristic(RegionCell a, RegionCell b, boolean wrapX) {
        int dx = Math.abs(b.x() - a.x());
        if (wrapX) {
            dx = Math.min(dx, 320 - dx);
        }
        int dz = Math.abs(b.z() - a.z());
        return (dx + dz) * 0.1D;
    }

    @Override
    public void close() {
        pathExecutor.shutdown();
    }
}
