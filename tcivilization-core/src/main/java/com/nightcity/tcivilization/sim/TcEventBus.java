package com.nightcity.tcivilization.sim;

import com.mojang.logging.LogUtils;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.api.event.TcEvent;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Main-thread core event publisher with a 256-events/tick budget, FIFO overflow,
 * monotonic sequence IDs, and nesting depth protection (03 §5.6, 33 §5.2).
 */
public final class TcEventBus {
    public static final int MAX_EVENTS_PER_TICK = 256;
    public static final int MAX_NESTING_DEPTH = 4;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TcEventBus GLOBAL = new TcEventBus(NeoForge.EVENT_BUS::post);

    private final Queue<TcEvent> queue = new ConcurrentLinkedQueue<>();
    private final AtomicLong sequenceGenerator = new AtomicLong(0L);
    private final ThreadLocal<Integer> nestingDepth = ThreadLocal.withInitial(() -> 0);
    private final Consumer<Event> sink;

    public TcEventBus(Consumer<Event> sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public static TcEventBus global() {
        return GLOBAL;
    }

    public long currentSequence() {
        return sequenceGenerator.get();
    }

    public void restoreSequence(long sequence) {
        sequenceGenerator.set(Math.max(0L, sequence));
    }

    public int pendingCount() {
        return queue.size();
    }

    public long enqueue(TcEvent event) {
        Objects.requireNonNull(event, "event");
        long seq = sequenceGenerator.incrementAndGet();
        event.assignSequence(seq);
        queue.add(event);
        return seq;
    }

    public <E extends TcEvent> E postImmediate(E event) {
        Objects.requireNonNull(event, "event");
        TcThreads.assertMain();
        if (event.sequence() == 0L) {
            event.assignSequence(sequenceGenerator.incrementAndGet());
        }
        dispatchWithDepthGuard(event);
        return event;
    }

    public int flushTick() {
        return flush(MAX_EVENTS_PER_TICK);
    }

    public int flush(int budget) {
        TcThreads.assertMain();
        int limit = Math.max(0, budget);
        int dispatched = 0;
        while (dispatched < limit) {
            TcEvent next = queue.poll();
            if (next == null) {
                break;
            }
            dispatchWithDepthGuard(next);
            dispatched++;
        }
        return dispatched;
    }

    public void clear() {
        queue.clear();
    }

    private void dispatchWithDepthGuard(Event event) {
        int depth = nestingDepth.get();
        if (depth >= MAX_NESTING_DEPTH) {
            if (TcThreads.isStrictMode()) {
                throw new IllegalStateException("TC event nesting depth exceeded " + MAX_NESTING_DEPTH + " on " + event.getClass().getSimpleName());
            }
            LOGGER.error("Dropping deeply nested TC event {} at depth {}", event.getClass().getSimpleName(), depth);
            return;
        }
        nestingDepth.set(depth + 1);
        try {
            sink.accept(event);
        } catch (RuntimeException ex) {
            if (TcThreads.isStrictMode()) {
                throw ex;
            }
            LOGGER.error("Subscriber threw while handling {}: {}", event.getClass().getSimpleName(), ex.getMessage(), ex);
        } finally {
            nestingDepth.set(depth);
        }
    }
}
