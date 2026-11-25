package com.nightcity.tcivilization.api.event;

import net.neoforged.bus.api.Event;

/**
 * Base class for core game-bus events carrying a monotonic per-world sequence ID (03 §5.6).
 */
public abstract class TcEvent extends Event {
    private long sequence;

    protected TcEvent() {
    }

    public final long sequence() {
        return sequence;
    }

    public final void assignSequence(long sequence) {
        if (this.sequence == 0L) {
            this.sequence = sequence;
        }
    }
}
