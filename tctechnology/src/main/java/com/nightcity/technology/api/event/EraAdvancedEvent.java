package com.nightcity.technology.api.event;

import com.nightcity.technology.api.TechEra;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.UUID;

public final class EraAdvancedEvent extends Event {
    public static final class Pre extends Event implements ICancellableEvent {
        private final UUID civ;
        private final TechEra from;
        private final TechEra to;
        private final UUID declaredBy;

        public Pre(UUID civ, TechEra from, TechEra to, UUID declaredBy) {
            this.civ = civ;
            this.from = from;
            this.to = to;
            this.declaredBy = declaredBy;
        }

        public UUID civ() {
            return civ;
        }

        public TechEra from() {
            return from;
        }

        public TechEra to() {
            return to;
        }

        public UUID declaredBy() {
            return declaredBy;
        }
    }

    private final UUID civ;
    private final TechEra from;
    private final TechEra to;
    private final long tick;
    private final int durationDays;

    public EraAdvancedEvent(UUID civ, TechEra from, TechEra to, long tick, int durationDays) {
        this.civ = civ;
        this.from = from;
        this.to = to;
        this.tick = tick;
        this.durationDays = durationDays;
    }

    public UUID civ() {
        return civ;
    }

    public TechEra from() {
        return from;
    }

    public TechEra to() {
        return to;
    }

    public long tick() {
        return tick;
    }

    public int durationDays() {
        return durationDays;
    }
}
